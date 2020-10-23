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

import org.sonar.check.Rule;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.AllBranchesIdenticalCheck;
import org.sonarsource.slang.checks.api.CheckContext;

@Rule(key = "S3923")
public class AllBranchesIdenticalScalaCheck extends AllBranchesIdenticalCheck {

  @Override
  protected void onAllIdenticalBranches(CheckContext ctx, Tree tree) {
    if (!hasPatternMatchCase(tree)) {
      ctx.reportIssue(tree, "Remove this conditional structure or edit its code blocks so that they're not all the same.");
    }
  }

  private static boolean hasPatternMatchCase(Tree tree) {
    return tree instanceof MatchTree &&
      ((MatchTree)tree).cases().stream().anyMatch(PatternMatchHelper::hasPatternMatchedVariable);
  }
}
