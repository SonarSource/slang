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

import java.util.List;

import org.sonar.check.Rule;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.HasKeyword;
import org.sonarsource.slang.api.JumpTree;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.ThrowTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;


@Rule(key = "S1763")
public class CodeAfterJumpCheck implements SlangCheck {
  private static final String MESSAGE = "Refactor this piece of code to not have any dead code after this \"%s\".";

  @Override
  public void initialize(InitContext init) {
    init.register(BlockTree.class, (ctx, blockTree) -> checkStatements(ctx, blockTree.statementOrExpressions()));
  }

  protected boolean isValidAfterJump(Tree tree) {
    return false;
  }

  protected boolean shouldIgnore(Tree tree) {
    return false;
  }

  private void checkStatements(CheckContext ctx, List<Tree> statementsOrExpressions) {
    if (statementsOrExpressions.size() < 2) {
      return;
    }

    int index = 0;
    while (index < statementsOrExpressions.size() - 1){
      Tree current = statementsOrExpressions.get(index);
      index++;

      Tree next = statementsOrExpressions.get(index);
      while (index < statementsOrExpressions.size() && shouldIgnore(next)){
        next = statementsOrExpressions.get(index);
        index++;
      }

      if (isJump(current) &&
         !shouldIgnore(next) &&
         !isValidAfterJump(next)) {
        ctx.reportIssue(current, String.format(MESSAGE, ((HasKeyword) current).keyword().text()));
      }
    }
  }

  private static boolean isJump(Tree tree){
    return tree instanceof JumpTree || tree instanceof ReturnTree || tree instanceof ThrowTree;
  }
}
