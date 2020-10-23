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
package org.sonarsource.ruby.converter.visitor;

import org.junit.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class ParenthesizedVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void test() {
    ParenthesizedExpressionTree parenthesizedTree = (ParenthesizedExpressionTree) rubyStatement("(a + b)");
    assertTree(parenthesizedTree.expression()).isBinaryExpression(Operator.PLUS);
    assertThat(parenthesizedTree.leftParenthesis().text()).isEqualTo("(");
    assertThat(parenthesizedTree.rightParenthesis().text()).isEqualTo(")");

    parenthesizedTree = (ParenthesizedExpressionTree) rubyStatement("(1)");
    assertTree(parenthesizedTree.expression()).isLiteral("1");
    assertThat(parenthesizedTree.leftParenthesis().text()).isEqualTo("(");
    assertThat(parenthesizedTree.rightParenthesis().text()).isEqualTo(")");
  }

  @Test
  public void not_expression_if_multiple_elements() {
    Tree beginAsStatementList = rubyStatement("(a; b;)");
    assertTree(beginAsStatementList).isBlock(NativeTree.class, NativeTree.class);
  }

}
