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

import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.MatchTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;

import static com.sonarsource.slang.checks.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S3923")
public class AllBranchesIdenticalCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, tree) -> {
      if (!(ctx.parent() instanceof IfTree)) {
        checkConditionalStructure(ctx, tree, new ConditionalStructure(tree));
      }
    });
    init.register(MatchTree.class, (ctx, tree) ->
      checkConditionalStructure(ctx, tree, new ConditionalStructure(tree))
    );
  }

  private static void checkConditionalStructure(CheckContext ctx, Tree tree, ConditionalStructure conditional) {
    if (conditional.allBranchesArePresent && conditional.allBranchesAreIdentical()) {
      ctx.reportIssue(tree, "Remove this conditional structure or edit its code blocks so that they're not all the same.");
    }
  }

  private static class ConditionalStructure {

    boolean allBranchesArePresent = false;

    private final List<Tree> branches = new ArrayList<>();

    private ConditionalStructure(IfTree ifTree) {
      branches.add(ifTree.thenBranch());
      Tree elseBranch = ifTree.elseBranch();
      while (elseBranch != null) {
        if (elseBranch instanceof IfTree) {
          IfTree elseIf = (IfTree) elseBranch;
          branches.add(elseIf.thenBranch());
          elseBranch = elseIf.elseBranch();
        } else {
          branches.add(elseBranch);
          allBranchesArePresent = true;
          elseBranch = null;
        }
      }
    }

    private ConditionalStructure(MatchTree tree) {
      for (MatchCaseTree caseTree : tree.cases()) {
        branches.add(caseTree.body());
        if (caseTree.expression() == null) {
          allBranchesArePresent = true;
        }
      }
    }

    private boolean allBranchesAreIdentical() {
      Tree first = branches.get(0);
      return branches.stream()
        .skip(1)
        .allMatch(branch -> areEquivalent(first, branch));
    }
  }

}
