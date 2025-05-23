/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.checks;

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.checks.utils.Language;
import org.sonarsource.slang.checks.utils.PropertyDefaultValue;

@Rule(key = "S117")
public class VariableAndParameterNameCheck implements SlangCheck {

  private static final String DEFAULT_FORMAT = "^[_a-z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the names against."
  )
  @PropertyDefaultValue(language = Language.RUBY, defaultValue = Language.RUBY_NAMING_DEFAULT)
  @PropertyDefaultValue(language = Language.SCALA, defaultValue = Language.SCALA_NAMING_DEFAULT)
  @PropertyDefaultValue(language = Language.GO, defaultValue = Language.GO_NAMING_DEFAULT)
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
      tree.formalParameters().stream()
        .filter(ParameterTree.class::isInstance)
        .map(ParameterTree.class::cast)
        .forEach(param -> check(pattern, ctx, param.identifier(), "parameter")));
  }

  private void check(Pattern pattern, CheckContext ctx, @Nullable IdentifierTree identifier, String variableKind) {
    if (identifier != null && !pattern.matcher(identifier.name()).matches()) {
      String message = String.format("Rename this %s to match the regular expression \"%s\".", variableKind, this.format);
      ctx.reportIssue(identifier, message);
    }
  }

}
