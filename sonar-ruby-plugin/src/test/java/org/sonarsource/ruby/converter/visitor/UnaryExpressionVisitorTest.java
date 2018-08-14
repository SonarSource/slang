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
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.api.UnaryExpressionTree.Operator;

import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class UnaryExpressionVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void negation() {
    UnaryExpressionTree unaryTree = (UnaryExpressionTree) rubyStatement("!a");
    assertTree(unaryTree).isUnaryExpression(Operator.NEGATE);
    assertTree(unaryTree).isEquivalentTo(rubyStatement("not a"));

    assertTree(rubyStatement("not 2")).isEquivalentTo(slangStatements("!2;").get(0));
  }

  @Test
  public void unary_minus() throws Exception {
    Tree tree = rubyStatement("-a");
    assertTree(tree).isInstanceOf(NativeTree.class);
  }

  @Test
  public void doubleNegation() throws Exception {
    UnaryExpressionTree unaryTree = (UnaryExpressionTree) rubyStatement("!!a");
    assertTree(unaryTree).isUnaryExpression(Operator.NEGATE);
    assertTree(unaryTree.operand()).isUnaryExpression(Operator.NEGATE);
  }

}
