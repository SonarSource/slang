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
package org.sonarsource.ruby.converter.visitor;


import org.junit.jupiter.api.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.api.UnaryExpressionTree.Operator;

import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class UnaryExpressionVisitorTest extends AbstractRubyConverterTest {

  @Test
  void test() {
    UnaryExpressionTree negation = (UnaryExpressionTree) rubyStatement("!a");
    assertTree(negation).isUnaryExpression(Operator.NEGATE);
    assertTree(negation).isEquivalentTo(rubyStatement("not a"));

    assertTree(rubyStatement("not 2")).isEquivalentTo(slangStatements("!2;").get(0));

    UnaryExpressionTree doubleNegation = (UnaryExpressionTree) rubyStatement("!!a");
    assertTree(doubleNegation).isUnaryExpression(Operator.NEGATE);
    assertTree(doubleNegation.operand()).isUnaryExpression(Operator.NEGATE);

    UnaryExpressionTree unaryPlus = (UnaryExpressionTree) rubyStatement("+a");
    assertTree(unaryPlus).isUnaryExpression(Operator.PLUS);
    assertTree(unaryPlus).isNotEquivalentTo(rubyStatement("-a"));

    UnaryExpressionTree unaryMinus = (UnaryExpressionTree) rubyStatement("-a");
    assertTree(unaryMinus).isUnaryExpression(Operator.MINUS);

    UnaryExpressionTree doublePlus = (UnaryExpressionTree) rubyStatement("++a");
    assertTree(doublePlus).isUnaryExpression(Operator.PLUS);
    assertTree(doublePlus.operand()).isUnaryExpression(Operator.PLUS);

    UnaryExpressionTree doubleMinus = (UnaryExpressionTree) rubyStatement("--a");
    assertTree(doubleMinus).isUnaryExpression(Operator.MINUS);
    assertTree(doubleMinus.operand()).isUnaryExpression(Operator.MINUS);
  }

}
