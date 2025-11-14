/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;

@Rule(key = "S1110")
public class RedundantParenthesesCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(ParenthesizedExpressionTree.class, (ctx, tree) -> {
      if (ctx.parent() instanceof ParenthesizedExpressionTree) {
        SecondaryLocation secondaryLocation = new SecondaryLocation(tree.rightParenthesis().textRange(), null);
        ctx.reportIssue(tree.leftParenthesis(), "Remove these useless parentheses.", secondaryLocation);
      }
    });
  }

}
