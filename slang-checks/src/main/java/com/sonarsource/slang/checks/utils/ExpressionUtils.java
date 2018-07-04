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
package com.sonarsource.slang.checks.utils;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.ParenthesizedExpressionTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.UnaryExpressionTree;
import java.util.Arrays;
import java.util.List;

public class ExpressionUtils {
  private static final String TRUE_LITERAL = "true";
  private static final String FALSE_LITERAL = "false";
  private static final List<String> BOOLEAN_LITERALS = Arrays.asList(TRUE_LITERAL, FALSE_LITERAL);

  private ExpressionUtils() {
  }

  public static boolean isBooleanLiteral(Tree tree) {
    return tree instanceof LiteralTree && BOOLEAN_LITERALS.contains(((LiteralTree) tree).value());
  }

  public static boolean isFalseValueLiteral(Tree originalTree) {
    Tree tree = skipParentheses(originalTree);
    return (tree instanceof LiteralTree && FALSE_LITERAL.equals(((LiteralTree) tree).value()))
      || (isNegation(tree) && isTrueValueLiteral(((UnaryExpressionTree) tree).operand()));
  }

  public static boolean isTrueValueLiteral(Tree originalTree) {
    Tree tree = skipParentheses(originalTree);
    return (tree instanceof LiteralTree && TRUE_LITERAL.equals(((LiteralTree) tree).value()))
      || (isNegation(tree) && isFalseValueLiteral(((UnaryExpressionTree) tree).operand()));
  }

  public static boolean isNegation(Tree tree) {
    return tree instanceof UnaryExpressionTree && ((UnaryExpressionTree) tree).operator() == UnaryExpressionTree.Operator.NEGATE;
  }

  public static boolean isBinaryOperation(Tree tree, BinaryExpressionTree.Operator operator) {
    return tree instanceof BinaryExpressionTree && ((BinaryExpressionTree) tree).operator() == operator;
  }

  public static Tree skipParentheses(Tree tree) {
    Tree result = tree;
    while (result instanceof ParenthesizedExpressionTree) {
      result = ((ParenthesizedExpressionTree) result).expression();
    }
    return result;
  }

}
