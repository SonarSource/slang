/*
 * SonarSource SLang
 * Copyright (C) 2018-2020 SonarSource SA
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
package org.sonarsource.scala.checks;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.DuplicateBranchCheck;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.utils.SyntacticEquivalence;

@Rule(key = "S1871")
public class DuplicateBranchScalaCheck extends DuplicateBranchCheck {

  // We ignore the duplicated blocks that have some pattern matching in the "case" clause,
  // to avoid FPs when the variable in the pattern shadows a variable from outside.
  @Override
  protected void checkDuplicatedBranches(CheckContext ctx, Tree matchTree, List<Tree> branches) {
    for (List<Tree> group : SyntacticEquivalence.findDuplicatedGroups(branches)) {

      final Optional<TreeAndIndex> originalBlock = findFirstCaseTreeWithoutPattern(matchTree, group);
      if (originalBlock.isPresent()) {
        int shouldSkip = originalBlock.get().index + 1;
        group.stream().skip(shouldSkip)
          .filter(DuplicateBranchCheck::spansMultipleLines)
          .filter(block -> !hasPatternMatchCondition(matchTree, block))
          .forEach(duplicated -> {
            TextRange originalRange = originalBlock.get().tree.metaData().textRange();
            ctx.reportIssue(
              duplicated,
              "This branch's code block is the same as the block for the branch on line " + originalRange.start().line() + ".",
              new SecondaryLocation(originalRange, "Original"));
          });
      }
    }
  }

  // Returns the first case block (and its index) that doesn't have a pattern in its "case" clause.
  private static Optional<TreeAndIndex> findFirstCaseTreeWithoutPattern(Tree matchTree, List<Tree> duplicatedCaseBlocks) {
    for (int i = 0; i < duplicatedCaseBlocks.size(); i++) {
      Tree caseBody = duplicatedCaseBlocks.get(i);
      if (!hasPatternMatchCondition(matchTree, caseBody)) {
        return Optional.of(new TreeAndIndex(caseBody, i));
      }
    }
    return Optional.empty();
  }

  private static boolean hasPatternMatchCondition(Tree matchTree, Tree caseBody) {
    if (matchTree instanceof MatchTree) {
      Optional<MatchCaseTree> matchCaseTree = getMatchCaseTree((MatchTree)matchTree, caseBody);
      return matchCaseTree.isPresent() && PatternMatchHelper.hasPatternMatchedVariable(matchCaseTree.get());
    }
    return false;
  }

  private static Optional<MatchCaseTree> getMatchCaseTree(MatchTree parent, Tree caseBody) {
    return parent.cases().stream().filter(c -> c.body() == caseBody).findFirst();
  }

  private static class TreeAndIndex {
    Tree tree;
    int index;
    TreeAndIndex(@Nullable Tree tree, int index) {
      this.tree = tree;
      this.index = index;
    }
  }
}
