/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource Sàrl
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

import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.checks.utils.Language;
import org.sonarsource.slang.checks.utils.PropertyDefaultValue;

@Rule(key = "S138")
public class TooLongFunctionCheck implements SlangCheck {

  private static final int DEFAULT_MAX = 100;
  private static final String DEFAULT_MAX_VALUE = "" + DEFAULT_MAX;

  @RuleProperty(
    key = "max",
    description = "Maximum authorized lines of code in a function"
  )
  @PropertyDefaultValue(language = Language.RUBY, defaultValue = DEFAULT_MAX_VALUE)
  @PropertyDefaultValue(language = Language.SCALA, defaultValue = DEFAULT_MAX_VALUE)
  @PropertyDefaultValue(language = Language.GO, defaultValue = "" + Language.GO_DEFAULT_MAXIMUM_FUNCTION_LENGTH)
  public int max = DEFAULT_MAX;

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, tree) -> {
      BlockTree body = tree.body();
      if (body == null) {
        return;
      }
      int numberOfLinesOfCode = body.metaData().linesOfCode().size();
      if (numberOfLinesOfCode > max) {
        String message = String.format(
          "This function has %s lines of code, which is greater than the %s authorized. Split it into smaller functions.",
          numberOfLinesOfCode,
          max);
        ctx.reportIssue(tree.rangeToHighlight(), message);
      }
    });
  }

}
