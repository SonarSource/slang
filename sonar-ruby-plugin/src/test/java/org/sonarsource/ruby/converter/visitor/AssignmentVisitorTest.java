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
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.AssignmentExpressionTree.Operator;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class AssignmentVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void test() throws Exception {
    assertTree(rubyStatement("a = 1")).isInstanceOf(AssignmentExpressionTree.class);
    assertTree(rubyStatement("a = b")).isInstanceOf(AssignmentExpressionTree.class);

    assertTree(rubyStatement("A = 1")).isInstanceOf(AssignmentExpressionTree.class);
    assertTree(rubyStatement("A += 1")).isInstanceOf(AssignmentExpressionTree.class);

    assertTree(rubyStatement("a[A] = 1")).isInstanceOf(AssignmentExpressionTree.class);
    assertTree(rubyStatement("a[1] = 1")).isInstanceOf(AssignmentExpressionTree.class);

    assertTree(rubyStatement("$a = 1")).isInstanceOf(AssignmentExpressionTree.class);
    assertTree(rubyStatement("$a += 1")).isInstanceOf(AssignmentExpressionTree.class);

    assertTree(rubyStatement("@a = 1")).isInstanceOf(AssignmentExpressionTree.class);
    assertTree(rubyStatement("@a += 1")).isInstanceOf(AssignmentExpressionTree.class);

    assertTree(rubyStatement("@@a += 1")).isInstanceOf(AssignmentExpressionTree.class);
    assertTree(rubyStatement("@@a = 1")).isInstanceOf(AssignmentExpressionTree.class);
  }

  @Test
  public void self_assignment() throws Exception {
    AssignmentExpressionTree tree = (AssignmentExpressionTree) rubyStatement("a = a");
    assertTree(tree.leftHandSide()).isEquivalentTo(tree.statementOrExpression());

    tree = (AssignmentExpressionTree) rubyStatement("A = A");
    assertTree(tree.leftHandSide()).isEquivalentTo(tree.statementOrExpression());

    tree = (AssignmentExpressionTree) rubyStatement("@a = @a");
    assertTree(tree.leftHandSide()).isEquivalentTo(tree.statementOrExpression());

    tree = (AssignmentExpressionTree) rubyStatement("@@a = @@a");
    assertTree(tree.leftHandSide()).isEquivalentTo(tree.statementOrExpression());

    tree = (AssignmentExpressionTree) rubyStatement("a[1, 2] = a[1, 2]");
    assertTree(tree.leftHandSide()).isEquivalentTo(tree.statementOrExpression());

    tree = (AssignmentExpressionTree) rubyStatement("a[1], b, c = a[1], b, c");
    assertTree(tree.leftHandSide()).isEquivalentTo(tree.statementOrExpression());

    tree = (AssignmentExpressionTree) rubyStatement("a.b()[foo(1)] = a.b()[foo(1)]");
    assertTree(tree.leftHandSide()).isEquivalentTo(tree.statementOrExpression());
  }

  @Test
  public void lhs_location() throws Exception {
    assertTree(rubyStatement("a = 1")).isInstanceOf(AssignmentExpressionTree.class);
    assertTree(rubyStatement("a[1, 2] = 1")).isInstanceOf(AssignmentExpressionTree.class);
  }

  @Test
  public void test_operator() throws Exception {
    Tree tree = rubyStatement("a = 1");
    assertTree(tree).isInstanceOf(AssignmentExpressionTree.class);
    AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
    assertThat(assignment.operator()).isEqualTo(Operator.EQUAL);
    assertTree(assignment.leftHandSide()).isIdentifier("a");
    assertTree(assignment.statementOrExpression()).isLiteral("1");

    tree = rubyStatement("a += 1");
    assertTree(tree).isInstanceOf(AssignmentExpressionTree.class);
    assignment = (AssignmentExpressionTree) tree;
    assertThat(assignment.operator()).isEqualTo(Operator.PLUS_EQUAL);
    assertTree(assignment.leftHandSide()).isIdentifier("a");
    assertTree(assignment.statementOrExpression()).isLiteral("1");
  }

  @Test
  public void nestedAssignment() throws Exception {
    Tree nestedAssignment = rubyStatement("x = y = 1");

    assertTree(nestedAssignment).isInstanceOf(AssignmentExpressionTree.class);
    assertTree(((AssignmentExpressionTree) nestedAssignment).statementOrExpression()).isInstanceOf(AssignmentExpressionTree.class);
  }

  @Test
  public void compound_are_natives() throws Exception {
    assertThat(((NativeTree) rubyStatement("a -= 1")).nativeKind()).isEqualTo(nativeKind("op_asgn"));
    assertThat(((NativeTree) rubyStatement("a *= 1")).nativeKind()).isEqualTo(nativeKind("op_asgn"));
    assertThat(((NativeTree) rubyStatement("a /= 1")).nativeKind()).isEqualTo(nativeKind("op_asgn"));
    assertThat(((NativeTree) rubyStatement("a **= 1")).nativeKind()).isEqualTo(nativeKind("op_asgn"));
    assertThat(((NativeTree) rubyStatement("a %= 1")).nativeKind()).isEqualTo(nativeKind("op_asgn"));
    assertThat(((NativeTree) rubyStatement("a <<= 1")).nativeKind()).isEqualTo(nativeKind("op_asgn"));
    assertThat(((NativeTree) rubyStatement("a >>= 1")).nativeKind()).isEqualTo(nativeKind("op_asgn"));
    assertThat(((NativeTree) rubyStatement("a |= 1")).nativeKind()).isEqualTo(nativeKind("op_asgn"));
    assertThat(((NativeTree) rubyStatement("a ^= 1")).nativeKind()).isEqualTo(nativeKind("op_asgn"));
    
    assertThat(((NativeTree) rubyStatement("a &&= 1")).nativeKind()).isEqualTo(nativeKind("and_asgn"));
    assertThat(((NativeTree) rubyStatement("a ||= 1")).nativeKind()).isEqualTo(nativeKind("or_asgn"));

    assertThat(((NativeTree) rubyStatement("::A = 1")).nativeKind()).isEqualTo(nativeKind("casgn"));
  }

}
