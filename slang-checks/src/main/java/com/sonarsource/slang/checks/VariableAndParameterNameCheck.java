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
package com.sonarsource.slang.checks;

import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.VariableDeclarationTree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

@Rule(key = "S117")
public class VariableAndParameterNameCheck implements SlangCheck {

  private static final String DEFAULT_FORMAT = "^[_a-z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the names against.",
    defaultValue = DEFAULT_FORMAT
  )
  public String format = DEFAULT_FORMAT;

  @Override
  public void initialize(InitContext init) {
    Pattern pattern = Pattern.compile(format);

    init.register(VariableDeclarationTree.class, (ctx, tree) -> {
      if (ctx.ancestors().stream().anyMatch(FunctionDeclarationTree.class::isInstance)) {
        check(pattern, ctx, tree.identifier(), "local variable");
      }
    });

    init.register(FunctionDeclarationTree.class, (ctx, tree) ->
      tree.formalParameters().forEach(
        param -> check(pattern, ctx, param.identifier(), "parameter")));
  }

  private void check(Pattern pattern, CheckContext ctx, IdentifierTree identifier, String variableKind) {
    if (!pattern.matcher(identifier.name()).matches()) {
      String message = String.format("Rename this %s to match the regular expression \"%s\".", variableKind, this.format);
      ctx.reportIssue(identifier, message);
    }
  }

}
