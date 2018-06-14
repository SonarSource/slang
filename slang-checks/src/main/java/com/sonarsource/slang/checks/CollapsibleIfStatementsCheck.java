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

import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SecondaryLocation;
import com.sonarsource.slang.checks.api.SlangCheck;
import java.util.Optional;
import org.sonar.check.Rule;

@Rule(key = "S1066")
public class CollapsibleIfStatementsCheck implements SlangCheck {
  private static final String MESSAGE = "Merge this if statement with the nested one.";
  private static final String SECONDARY_MESSAGE = "Nested \"if\" statement";

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, ifTreeStatement) -> {
      if (ifTreeStatement.elseBranch() == null) {
        getCollapsibleIfStatement(ifTreeStatement.thenBranch())
          .ifPresent(innerIfStatement -> {
            TextRange innerIfRange = innerIfStatement.metaData().textRange();
            ctx.reportIssue(ifTreeStatement, MESSAGE, new SecondaryLocation(innerIfRange, SECONDARY_MESSAGE));
          });
      }
    });
  }

  private static Optional<IfTree> getCollapsibleIfStatement(Tree tree) {
    if (tree instanceof BlockTree) {
      BlockTree blockTree = (BlockTree) tree;
      return blockTree.statementOrExpressions().size() == 1
        ? getIfStatementWithoutElse(tree.children().get(0))
        : Optional.empty();
    }
    return getIfStatementWithoutElse(tree);
  }

  private static Optional<IfTree> getIfStatementWithoutElse(Tree tree) {
    return tree instanceof IfTree && ((IfTree) tree).elseBranch() == null
      ? Optional.of((IfTree) tree)
      : Optional.empty();
  }
}
