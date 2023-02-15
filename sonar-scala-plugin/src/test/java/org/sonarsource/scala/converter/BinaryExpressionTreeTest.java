/*
 * SonarSource SLang
 * Copyright (C) 2018-2023 SonarSource SA
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
package org.sonarsource.scala.converter;

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;

import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class BinaryExpressionTreeTest extends AbstractScalaConverterTest {
  @Test
  void comparison_operators() {
    Tree binaryExpressionTree = scalaStatement("a == 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.EQUAL_TO);

    binaryExpressionTree = scalaStatement("a != 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.NOT_EQUAL_TO);

    binaryExpressionTree = scalaStatement("a > 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.GREATER_THAN);

    binaryExpressionTree = scalaStatement("a >= 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.GREATER_THAN_OR_EQUAL_TO);

    binaryExpressionTree = scalaStatement("a < 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.LESS_THAN);

    binaryExpressionTree = scalaStatement("a <= 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.LESS_THAN_OR_EQUAL_TO);
  }

  @Test
  void arithmetic_operators() {
    Tree binaryExpressionTree = scalaStatement("a + 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.PLUS);

    binaryExpressionTree = scalaStatement("a - 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.MINUS);

    binaryExpressionTree = scalaStatement("a * 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.TIMES);

    binaryExpressionTree = scalaStatement("a / 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.DIVIDED_BY);
  }

  @Test
  void logical_operators() {
    Tree binaryExpressionTree = scalaStatement("a && 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.CONDITIONAL_AND);

    binaryExpressionTree = scalaStatement("a || 2");
    assertTree(binaryExpressionTree).isBinaryExpression(BinaryExpressionTree.Operator.CONDITIONAL_OR);
  }

  @Test
  void mapped_to_native() {
    Tree tree = scalaStatement("foo * (bar, baz)");
    assertTree(tree).isInstanceOf(NativeTree.class);

    tree = scalaStatement("a foo 2");
    assertTree(tree).isInstanceOf(NativeTree.class);
  }
  @Test
  void placeholder() {
    Tree tree = scalaStatement("_ * _");
    // The binary expression is wrapped into an anonymous function declaration
    assertTree(tree.children().get(0)).isInstanceOf(BinaryExpressionTree.class);

    tree = scalaStatement("_ + 2");
    assertTree(tree.children().get(0)).isInstanceOf(BinaryExpressionTree.class);

    tree = scalaStatement("_._1 < _._1");
    assertTree(tree.children().get(0)).isInstanceOf(BinaryExpressionTree.class);

    tree = scalaStatement("_._1 < a._1");
    assertTree(tree.children().get(0)).isInstanceOf(BinaryExpressionTree.class);

    tree = scalaStatement("_._1._1 < _._1._1");
    assertTree(tree.children().get(0)).isInstanceOf(BinaryExpressionTree.class);

    tree = scalaStatement("a._1._1 < _._1._1");
    assertTree(tree.children().get(0)).isInstanceOf(BinaryExpressionTree.class);
  }
}
