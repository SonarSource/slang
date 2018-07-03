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

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.ParenthesizedExpressionTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.UnaryExpressionTree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

@Rule(key = "S1067")
public class TooComplexExpressionCheck implements SlangCheck {

  private static final int DEFAULT_MAX_COMPLEXITY = 3;

  @RuleProperty(key = "max",
    description = "Maximum number of allowed conditional operators in an expression",
    defaultValue = "" + DEFAULT_MAX_COMPLEXITY)
  public int max = DEFAULT_MAX_COMPLEXITY;

  @Override
  public void initialize(InitContext init) {
    init.register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (isParentExpression(ctx)) {
        int complexity = computeExpressionComplexity(tree);
        if (complexity > max) {
          String message = String.format(
            "Reduce the number of conditional operators (%s) used in the expression (maximum allowed %s).",
            complexity,
            max);
          double gap = (double) complexity - max;
          ctx.reportIssue(tree, message, Collections.emptyList(), gap);
        }
      }
    });
  }

  private static boolean isParentExpression(CheckContext ctx) {
    Iterator<Tree> iterator = ctx.ancestors().iterator();
    while (iterator.hasNext()) {
      Tree parentExpression = iterator.next();
      if (parentExpression instanceof BinaryExpressionTree) {
        return false;
      } else if (!(parentExpression instanceof UnaryExpressionTree) || !(parentExpression instanceof ParenthesizedExpressionTree)) {
        return true;
      }
    }
    return true;
  }

  private static int computeExpressionComplexity(BinaryExpressionTree tree) {
    List<Tree> complexityTrees = new ArrayList<>();
    TreeVisitor<TreeContext> binaryExpressionVisitor = new TreeVisitor<>();
    binaryExpressionVisitor.register(BinaryExpressionTree.class, (ctx, binaryTree) -> {
      if (binaryTree.operator() == BinaryExpressionTree.Operator.CONDITIONAL_AND ||
        binaryTree.operator() == BinaryExpressionTree.Operator.CONDITIONAL_OR) {
        complexityTrees.add(binaryTree);
      }
    });
    binaryExpressionVisitor.scan(new TreeContext(), tree);
    return complexityTrees.size();
  }

}
