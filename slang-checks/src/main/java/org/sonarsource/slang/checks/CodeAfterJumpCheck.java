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

  private static void checkStatements(CheckContext ctx, List<Tree> statementsOrExpressions) {
    if (statementsOrExpressions.size() < 2) {
      return;
    }
    statementsOrExpressions.subList(0, statementsOrExpressions.size() - 1).stream()
      .filter(tree -> tree instanceof JumpTree || tree instanceof ReturnTree || tree instanceof ThrowTree)
      .forEach(tree -> ctx.reportIssue(tree, String.format(MESSAGE, ((HasKeyword) tree).keyword().text())));
  }

}
