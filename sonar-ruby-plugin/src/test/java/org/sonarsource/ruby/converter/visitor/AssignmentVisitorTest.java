/*
 * SonarSource SLang
 * Copyright (C) 2018-2022 SonarSource SA
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
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.AssignmentExpressionTree.Operator;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.VariableDeclarationTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class AssignmentVisitorTest extends AbstractRubyConverterTest {

  @Test
  void test() throws Exception {
    assertTree(rubyStatement("a = 1")).isInstanceOf(VariableDeclarationTree.class);
    assertTree(rubyStatement("a = b")).isInstanceOf(VariableDeclarationTree.class);

    assertTree(rubyStatement("A = 1")).isInstanceOf(VariableDeclarationTree.class);
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
  void self_assignment() throws Exception {
    VariableDeclarationTree var = (VariableDeclarationTree) rubyStatement("a = a");
    assertTree(var.identifier()).isEquivalentTo(var.initializer());

    AssignmentExpressionTree tree = (AssignmentExpressionTree) rubyStatement("A = 1\nA = A").children().get(1);
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
  void lhs_location() throws Exception {
    assertTree(rubyStatement("a = 1")).isInstanceOf(VariableDeclarationTree.class);
    assertTree(rubyStatement("a = 0;a = 1").children().get(1)).isInstanceOf(AssignmentExpressionTree.class);
    assertTree(rubyStatement("a[1, 2] = 1")).isInstanceOf(AssignmentExpressionTree.class);
  }

  @Test
  void test_operator() throws Exception {
    Tree block = rubyStatement("a = 0\na = 1");
    Tree tree = block.children().get(1);
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
  void nestedAssignment() throws Exception {
    Tree nestedAssignment = rubyStatement("x = y = 1");

    assertTree(nestedAssignment).isInstanceOf(VariableDeclarationTree.class);
    assertTree(((VariableDeclarationTree) nestedAssignment).initializer()).isInstanceOf(VariableDeclarationTree.class);
  }

  @Test
  void multiple_declaration_or_assignment() throws Exception {
    Tree tree = rubyStatement("a, b = 0, 1\na, b = 0, 1");
    assertThat(tree.children()).hasSize(2);
    assertTree(tree.children().get(0)).isInstanceOf(NativeTree.class);
    assertTree(tree.children().get(1)).isInstanceOf(NativeTree.class);

    Tree twoDeclaration = tree.children().get(0);
    assertThat(twoDeclaration.children()).hasSize(2);

    assertTree(twoDeclaration.children().get(0)).isInstanceOf(VariableDeclarationTree.class);
    VariableDeclarationTree declarationTree = (VariableDeclarationTree) twoDeclaration.children().get(0);
    assertTree(declarationTree.identifier()).isIdentifier("a");
    assertTree(declarationTree.initializer()).isLiteral("0");

    assertTree(twoDeclaration.children().get(1)).isInstanceOf(VariableDeclarationTree.class);
    assertThat(twoDeclaration.children()).hasSize(2);
    VariableDeclarationTree declarationTree2 = (VariableDeclarationTree) twoDeclaration.children().get(1);
    assertTree(declarationTree2.identifier()).isIdentifier("b");
    assertTree(declarationTree2.initializer()).isLiteral("1");

    Tree twoAssignment = tree.children().get(1);
    assertThat(twoAssignment.children()).hasSize(2);

    assertTree(twoAssignment.children().get(0)).isInstanceOf(AssignmentExpressionTree.class);
    AssignmentExpressionTree assignmentTree = (AssignmentExpressionTree) twoAssignment.children().get(0);
    assertTree(assignmentTree.leftHandSide()).isIdentifier("a");
    assertTree(assignmentTree.statementOrExpression()).isLiteral("0");

    assertTree(twoAssignment.children().get(1)).isInstanceOf(AssignmentExpressionTree.class);
    AssignmentExpressionTree assignmentTree2 = (AssignmentExpressionTree) twoAssignment.children().get(1);
    assertTree(assignmentTree2.leftHandSide()).isIdentifier("b");
    assertTree(assignmentTree2.statementOrExpression()).isLiteral("1");
  }

  @Test
  void compound_are_natives() throws Exception {
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
