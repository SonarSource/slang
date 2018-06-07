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

package com.sonarsource.slang.antlr;

import com.sonarsource.slang.api.AssignmentExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.Comment;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.MatchTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.parser.SLangConverter;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.antlr.v4.runtime.CharStreams;
import org.junit.Test;

import static com.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.assertj.core.api.Assertions.assertThat;

public class SLangConverterTest {

  private SLangConverter converter = new SLangConverter();

  @Test
  public void testConverter() throws IOException {
    Tree tree = converter.parse(CharStreams.fromFileName("src/test/resources/binary.slang").toString());

    AtomicInteger numBinNodes = new AtomicInteger(0);
    AtomicInteger numIdentifierNode = new AtomicInteger(0);
    AtomicInteger numLiteralNode = new AtomicInteger(0);

    TreeVisitor<TreeContext> visitor = new TreeVisitor<>();

    visitor.register(BinaryExpressionTreeImpl.class, (ctx, binaryExpressionTree) -> numBinNodes.getAndIncrement());
    visitor.register(IdentifierTreeImpl.class, (ctx, identifierTree) -> numIdentifierNode.getAndIncrement());
    visitor.register(LiteralTreeImpl.class, (ctx, literalTree) -> numLiteralNode.getAndIncrement());
    visitor.scan(new TreeContext(), tree);

    assertTree(tree).isInstanceOf(TopLevelTree.class).hasChildren(9);
    assertThat(numBinNodes.get()).isEqualTo(6);
    assertThat(numIdentifierNode.get()).isEqualTo(18);
    assertThat(numLiteralNode.get()).isEqualTo(10);
  }

  @Test
  public void simple_binary_expression() {
    BinaryExpressionTree binary = parseBinary("x + 1");
    assertTree(binary).isBinaryExpression(Operator.PLUS).hasTextRange(1, 0, 1, 5);
    assertTree(binary.leftOperand()).isIdentifier("x").hasTextRange(1, 0, 1, 1);
    assertTree(binary.rightOperand()).isLiteral("1").hasTextRange(1, 4, 1, 5);
  }

  @Test
  public void conditional_and_with_multiple_operands() {
    BinaryExpressionTree binary = parseBinary("x && y && z");
    assertTree(binary).isBinaryExpression(Operator.CONDITIONAL_AND);
    assertTree(binary.leftOperand()).isIdentifier("x");
    assertTree(binary.rightOperand()).isBinaryExpression(Operator.CONDITIONAL_AND);
  }

  @Test
  public void additive_expression_with_multiple_operands() {
    BinaryExpressionTree binary = parseBinary("x + y\n- z");
    assertTree(binary).isBinaryExpression(Operator.PLUS);
    assertTree(binary.leftOperand()).isIdentifier("x");
    assertTree(binary.rightOperand()).isBinaryExpression(Operator.MINUS).hasTextRange(1, 4, 2, 3);
  }

  @Test
  public void function() {
    FunctionDeclarationTree function = parseFunction("private int foo(x1, x2) { x1 + x2 }");
    assertThat(function.name().name()).isEqualTo("foo");
    // assertTree(function.returnType()).isIdentifier("boolean");
    assertThat(function.formalParameters()).hasSize(2);
    assertTree(function.formalParameters().get(0)).isIdentifier("x1");
    assertThat(function.body()).isNotNull();

    assertThat(parseFunction("int foo(p1);").formalParameters()).hasSize(1);
    assertTree(parseFunction("int foo(p1);").formalParameters().get(0)).isIdentifier("p1");

    assertThat(parseFunction("int foo();").formalParameters()).isEmpty();
    assertThat(parseFunction("int foo();").body()).isNull();

    assertThat(parseFunction("int foo() {}").body().statementOrExpressions()).isEmpty();
  }

  @Test
  public void if_without_else() {
    Tree tree = converter.parse("if (x > 0) { x = 1; }").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree).hasTextRange(1, 0, 1, 21);
    assertTree(ifTree.condition()).isBinaryExpression(Operator.GREATER_THAN);
    assertThat(ifTree.elseBranch()).isNull();
  }

  @Test
  public void if_with_else() {
    Tree tree = converter.parse("if (x > 0) { x == 1; } else { y }").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree).hasTextRange(1, 0, 1, 33);
    assertTree(ifTree.condition()).isBinaryExpression(Operator.GREATER_THAN);
    assertTree(ifTree.thenBranch()).isBlock(BinaryExpressionTree.class).hasTextRange(1, 11, 1, 22);
    assertTree(ifTree.elseBranch()).isBlock(IdentifierTree.class);
  }

  @Test
  public void if_with_else_if() {
    Tree tree = converter.parse("if (x > 0) { x == 1; } else if (x < 1) { y }").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree.elseBranch()).isInstanceOf(IfTree.class);
  }

  @Test
  public void match() {
    Tree tree = converter.parse("match(x) { 1 -> a; else -> b; }").children().get(0);
    assertTree(tree).isInstanceOf(MatchTree.class).hasTextRange(1, 0, 1, 31);
    MatchTree matchTree = (MatchTree) tree;
    assertTree(matchTree.expression()).isIdentifier("x");
    assertThat(matchTree.cases()).hasSize(2);
    assertTree(matchTree.cases().get(0).expression()).isLiteral("1");
    assertTree(matchTree.cases().get(1).expression()).isNull();
    assertTree(matchTree.cases().get(1)).hasTextRange(1, 19, 1, 29);
  }

  @Test
  public void natives() {
    Tree tree = converter.parse("native [] {}").children().get(0);
    assertTree(tree).isInstanceOf(NativeTree.class).hasTextRange(1, 0, 1, 12);

    tree = converter.parse("native [] { [x] } = x").children().get(0);
    assertTree(tree).isAssignmentExpression(AssignmentExpressionTree.Operator.EQUAL);
    AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
    assertTree(assignment.leftHandSide()).isInstanceOf(NativeTree.class).hasTextRange(1, 0, 1, 17);
  }

  @Test
  public void simple_assignment() {
    Tree tree = converter.parse("x = 1").children().get(0);
    assertTree(tree).isAssignmentExpression(AssignmentExpressionTree.Operator.EQUAL).hasTextRange(1, 0, 1, 5);
  }

  @Test
  public void nested_assignments() {
    Tree tree = converter.parse("x -= y += 2").children().get(0);
    assertTree(tree).isAssignmentExpression(AssignmentExpressionTree.Operator.MINUS_EQUAL).hasTextRange(1, 0, 1, 11);
    AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
    assertTree(assignment.leftHandSide()).isIdentifier("x");
    assertTree(assignment.statementOrExpression()).isAssignmentExpression(AssignmentExpressionTree.Operator.PLUS_EQUAL).hasTextRange(1, 5, 1, 11);
  }

  @Test
  public void top_level_tree() {
    Tree tree1 = converter.parse("int foo(p1);\nx == 3;");
    Tree tree2 = converter.parse("x + y\n\n- z");
    Tree emptyTree = converter.parse("");
    assertTree(tree1)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren(FunctionDeclarationTree.class, BinaryExpressionTree.class)
      .hasTextRange(1, 0, 2, 7);
    assertTree(tree2)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren(BinaryExpressionTree.class)
      .hasTextRange(1, 0, 3, 3);
    assertTree(emptyTree)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren()
      .hasTextRange(1, 0, 1, 0);
  }

  @Test
  public void comments() {
    BinaryExpressionTree binary = parseBinary("/* comment1 */ x /* comment2 */ == // comment3\n1");
    List<Comment> comments = binary.metaData().commentsInside();
    assertThat(comments).hasSize(2);
    Comment comment = comments.get(0);
    assertThat(comment.textRange().start().lineOffset()).isEqualTo(17);
    assertThat(comment.textWithDelimiters()).isEqualTo("/* comment2 */");
    assertThat(comment.text()).isEqualTo(" comment2 ");
    assertThat(comments.get(1).text()).isEqualTo(" comment3");
  }

  @Test
  public void decimalLiterals() {
    Tree tree = converter.parse("0; 5; 10; 123; 1010; 5554; 12345567;");
    String[] values = {"0", "5", "10", "123", "1010", "5554", "12345567"};

    assertTree(tree).isNotNull();
    assertTree(tree).isInstanceOf(TopLevelTree.class);
    TopLevelTree topLevelTree = (TopLevelTree) tree;
    assertThat(topLevelTree.declarations()).hasSize(7);

    for (int i = 0; i < topLevelTree.declarations().size(); i++) {
      assertTree(topLevelTree.declarations().get(i)).isLiteral(values[i]);
    }
  }

  private BinaryExpressionTree parseBinary(String code) {
    return (BinaryExpressionTree) parseExpressionOrStatement(code);
  }

  private Tree parseExpressionOrStatement(String code) {
    Tree tree = converter.parse(code);
    return tree.children().get(0);
  }

  private FunctionDeclarationTree parseFunction(String code) {
    return (FunctionDeclarationTree) converter.parse(code).children().get(0);
  }

}
