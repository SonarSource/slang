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

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.MatchTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.parser.SLangConverter;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreePrinter;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.antlr.v4.runtime.CharStreams;
import org.assertj.core.api.AbstractAssert;
import org.junit.Test;

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
    //assertTree(function.returnType()).isIdentifier("boolean");
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
    Tree tree = converter.parse("x = 1").children().get(0);
    assertTree(tree).isInstanceOf(NativeTree.class).hasTextRange(1, 0, 1, 5);
  }

  @Test
  public void top_level_tree() {
    Tree tree1 = converter.parse("int foo(p1);\nx == 3;");
    Tree tree2 = converter.parse("x + y\n\n- z");
    Tree emptyTree = converter.parse("");
    assertTree(tree1)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren(FunctionDeclarationTree.class, BinaryExpressionTree.class)
      .hasTextRange(1,0, 2, 7);
    assertTree(tree2)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren(BinaryExpressionTree.class)
      .hasTextRange(1,0, 3, 3);
    assertTree(emptyTree)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren()
      .hasTextRange(1, 0, 1, 0);
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

  public static class TreeAssert extends AbstractAssert<TreeAssert, Tree> {

    public TreeAssert(Tree actual) {
      super(actual, TreeAssert.class);
    }

    public TreeAssert isIdentifier(String expectedName) {
      isNotNull();
      isInstanceOf(IdentifierTree.class);
      IdentifierTree actualIdentifier = (IdentifierTree) actual;
      if (!Objects.equals(actualIdentifier.name(), expectedName)) {
        failWithMessage("Expected identifier's name to be <%s> but was <%s>", expectedName, actualIdentifier.name());
      }
      return this;
    }

    public TreeAssert isLiteral(String expected) {
      isNotNull();
      isInstanceOf(LiteralTree.class);
      LiteralTree actualLiteral = (LiteralTree) actual;
      if (!Objects.equals(actualLiteral.value(), expected)) {
        failWithMessage("Expected literal value to be <%s> but was <%s>", expected, actualLiteral.value());
      }
      return this;
    }

    public TreeAssert isBinaryExpression(Operator expectedOperator) {
      isNotNull();
      isInstanceOf(BinaryExpressionTree.class);
      BinaryExpressionTree actualBinary = (BinaryExpressionTree) actual;
      if (!Objects.equals(actualBinary.operator(), expectedOperator)) {
        failWithMessage("Expected operator to be <%s> but was <%s>", expectedOperator, actualBinary.operator());
      }
      return this;
    }

    public TreeAssert isBlock(Class... classes) {
      isNotNull();
      isInstanceOf(BlockTree.class);
      hasChildren(classes);
      return this;
    }

    public TreeAssert hasChildren(Class... classes) {
      hasChildren(classes.length);
      for (int i = 0; i < actual.children().size(); i++) {
        Tree tree = actual.children().get(i);
        if (!classes[i].isAssignableFrom(tree.getClass())) {
          failWithMessage("Expected to find instance of <%s> but was <%s>", classes[i], tree.getClass());
        }
      }
      return this;
    }

    public TreeAssert hasChildren(int count) {
      isNotNull();
      if (actual.children().size() != count) {
        failWithMessage("Expected to have <%s> children elements but found <%s>", count, actual.children().size());
      }
      return this;
    }

    public TreeAssert hasTextRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
      isNotNull();
      TextRange range = actual.textRange();
      assertThat(range.start().line()).isEqualTo(startLine);
      assertThat(range.start().lineOffset()).isEqualTo(startLineOffset);
      assertThat(range.end().line()).isEqualTo(endLine);
      assertThat(range.end().lineOffset()).isEqualTo(endLineOffset);
      return this;
    }

  }

  public static TreeAssert assertTree(Tree actual) {
    return new TreeAssert(actual);
  }
}
