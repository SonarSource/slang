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
package org.sonarsource.ruby.converter.visitor;


import org.junit.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;

import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class BinaryExpressionVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void comparison() {
    assertTree(rubyStatement("a == b")).isBinaryExpression(Operator.EQUAL_TO);
    assertTree(rubyStatement("a != b")).isBinaryExpression(Operator.NOT_EQUAL_TO);
    assertTree(rubyStatement("a < b")).isBinaryExpression(Operator.LESS_THAN);
    assertTree(rubyStatement("a > b")).isBinaryExpression(Operator.GREATER_THAN);
    assertTree(rubyStatement("a <= b")).isBinaryExpression(Operator.LESS_THAN_OR_EQUAL_TO);
    assertTree(rubyStatement("a >= b")).isBinaryExpression(Operator.GREATER_THAN_OR_EQUAL_TO);
  }

  @Test
  public void arithmetic() {
    assertTree(rubyStatement("a + b")).isBinaryExpression(Operator.PLUS);
    assertTree(rubyStatement("a - b")).isBinaryExpression(Operator.MINUS);
    assertTree(rubyStatement("a * b")).isBinaryExpression(Operator.TIMES);
    assertTree(rubyStatement("a / b")).isBinaryExpression(Operator.DIVIDED_BY);
  }

  @Test
  public void logical() {
    assertTree(rubyStatement("a && b")).isBinaryExpression(Operator.CONDITIONAL_AND);
    assertTree(rubyStatement("a || b")).isBinaryExpression(Operator.CONDITIONAL_OR);
    // NOTE: pairs &&/and and ||/or don't have the same priority, still the same tree is created
    assertTree(rubyStatement("a and b")).isBinaryExpression(Operator.CONDITIONAL_AND);
    assertTree(rubyStatement("a or b")).isBinaryExpression(Operator.CONDITIONAL_OR);
  }

  @Test
  public void equivalent_to_slang() {
    assertTree(rubyStatement("1 < 2")).isEquivalentTo(slangStatements("1 < 2;").get(0));
  }

}
