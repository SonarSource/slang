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
package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KotlinParserTest {

  @Test
  public void testFile() throws IOException {
    Tree tree = KotlinParser.fromFile("src/test/resources/test.kt");
    assertThat(tree).isNotNull();
  }

  @Test
  public void testString() {
    Tree tree = KotlinParser.fromString("fun function1() = 2 >= 1");
    assertThat(tree).isNotNull();
  }

  @Test
  public void testBinaryExpression() {
    List<Tree> allNodes = new LinkedList<>();
    TreeVisitor<TreeContext> visitor = new TreeVisitor<>();
    visitor.register(Tree.class, (ctx, tree) -> allNodes.add(tree));
    visitor.scan(new TreeContext(), KotlinParser.fromString("fun function1() = (1 + 2 * 3 * 4 / 5 - 6) == 32"));
    assertHasXNodesOfOperatorType(allNodes, Operator.PLUS, 1);
    assertHasXNodesOfOperatorType(allNodes, Operator.TIMES, 2);
    assertHasXNodesOfOperatorType(allNodes, Operator.DIVIDED_BY, 1);
    assertHasXNodesOfOperatorType(allNodes, Operator.MINUS, 1);
    assertHasXNodesOfOperatorType(allNodes, Operator.EQUAL_TO, 1);
  }

  private static void assertHasXNodesOfOperatorType(List<Tree> nodes, Operator operator, int expectedNumber) {
    assertThat(nodes)
      .filteredOn(node -> node instanceof BinaryExpressionTree && ((BinaryExpressionTree) node).operator() == operator)
      .hasSize(expectedNumber);
  }

}
