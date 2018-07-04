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
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.UnaryExpressionTree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.checks.utils.ExpressionUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;

@Rule(key = "S1125")
public class BooleanLiteralCheck implements SlangCheck {
  private static final List<BinaryExpressionTree.Operator> CONDITIONAL_BINARY_OPERATORS = Arrays.asList(
    BinaryExpressionTree.Operator.CONDITIONAL_AND,
    BinaryExpressionTree.Operator.CONDITIONAL_OR,
    BinaryExpressionTree.Operator.EQUAL_TO,
    BinaryExpressionTree.Operator.NOT_EQUAL_TO);

  private static final String MESSAGE = "Remove the unnecessary Boolean literal.";

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, ifTree) -> getBooleanLiteral(ifTree.thenBranch(), ifTree.elseBranch())
      .ifPresent(booleanLiteral -> ctx.reportIssue(booleanLiteral, MESSAGE)));

    init.register(BinaryExpressionTree.class, (ctx, binaryExprTree) -> {
      if (CONDITIONAL_BINARY_OPERATORS.contains(binaryExprTree.operator())) {
        getBooleanLiteral(binaryExprTree.leftOperand(), binaryExprTree.rightOperand())
          .ifPresent(booleanLiteral -> ctx.reportIssue(booleanLiteral, MESSAGE));
      }
    });

    init.register(UnaryExpressionTree.class, (ctx, unaryExprTree) -> {
      if (UnaryExpressionTree.Operator.NEGATE.equals(unaryExprTree.operator())) {
        getBooleanLiteral(unaryExprTree.operand())
          .ifPresent(booleanLiteral -> ctx.reportIssue(booleanLiteral, MESSAGE));
      }
    });
  }

  private static Optional<Tree> getBooleanLiteral(Tree... trees) {
    return Arrays.stream(trees)
      .map(ExpressionUtils::skipParentheses)
      .filter(ExpressionUtils::isBooleanLiteral)
      .findFirst();
  }
}
