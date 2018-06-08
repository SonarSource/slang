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
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SecondaryLocation;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.checks.utils.SyntacticEquivalence;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sonar.check.Rule;

@Rule(key = "S1862")
public class IdenticalConditionsCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(MatchTree.class, (ctx, tree) -> checkConditions(ctx, collectConditions(tree)));
    init.register(IfTree.class, (ctx, tree) -> {
      if (!(ctx.parent() instanceof IfTree)) {
        checkConditions(ctx, collectConditions(tree, new ArrayList<>()));
      }
    });
  }

  private static List<Tree> collectConditions(MatchTree matchTree) {
    return matchTree.cases().stream()
      .map(MatchCaseTree::expression)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private static List<Tree> collectConditions(IfTree ifTree, List<Tree> list) {
    list.add(ifTree.condition());
    Tree elseBranch = ifTree.elseBranch();
    if (elseBranch instanceof IfTree) {
      return collectConditions((IfTree) elseBranch, list);
    }
    return list;
  }

  private static void checkConditions(CheckContext ctx, List<Tree> conditions) {
    for (List<Tree> group : SyntacticEquivalence.findDuplicatedGroups(conditions)) {
      Tree original = group.get(0);
      group.stream().skip(1)
        .forEach(duplicated -> {
          TextRange originalRange = original.metaData().textRange();
          ctx.reportIssue(
            duplicated,
            "This condition duplicates the one on line " + originalRange.start().line() + ".",
            new SecondaryLocation(originalRange, "Original"));
        });
    }
  }
}
