/*
 * SonarSource SLang
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.slang.checks;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.checks.utils.Language;

@Rule(key = "S100")
public class BadFunctionNameCheck implements SlangCheck {

  public static final String DEFAULT_FORMAT = "^[a-z][a-zA-Z0-9]*$";

  private static final Map<Language, String> DEFAULT_BY_LANGUAGE;
  static {
    EnumMap<Language, String> defaults = new EnumMap<>(Language.class);
    defaults.put(Language.KOTLIN, DEFAULT_FORMAT);
    defaults.put(Language.RUBY, "^(@{0,2}[\\da-z_]+[!?=]?)|([*+-/%=!><~]+)|(\\[]=?)$");
    DEFAULT_BY_LANGUAGE = Collections.unmodifiableMap(defaults);
  }

  public static String getDefaultFormat(Language language) {
    return DEFAULT_BY_LANGUAGE.get(language);
  }

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the function names against."
  )
  public String format = DEFAULT_FORMAT;

  private String message(String name) {
    return "Rename function '" + name + "' to match the regular expression " + format;
  }

  @Override
  public void initialize(InitContext init) {
    Pattern pattern = Pattern.compile(format);
    init.register(FunctionDeclarationTree.class, (ctx, fnDeclarationTree) -> {
      IdentifierTree name = fnDeclarationTree.name();
      if (name != null && !pattern.matcher(name.name()).matches()) {
        ctx.reportIssue(fnDeclarationTree.name(), message(name.name()));
      }
    });
  }
}
