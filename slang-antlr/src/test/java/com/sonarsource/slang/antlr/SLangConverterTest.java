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
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.parser.SLangConverter;
import com.sonarsource.slang.visitors.TreeContext;
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

    assertThat(numBinNodes.get()).isEqualTo(6);
    assertThat(numIdentifierNode.get()).isEqualTo(8);
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
  }

  @Test
  public void native_kind() {
    Tree root = converter.parse("x == 1");
    Tree root2 = converter.parse("x == 2");
    Tree child = root.children().get(0);
    assertThat(root).isInstanceOf(NativeTree.class);
    assertThat(child).isInstanceOf(NativeTree.class);
    assertThat(((NativeTree) root).nativeKind()).isNotEqualTo(((NativeTree) child).nativeKind());
    assertThat(((NativeTree) root).nativeKind()).isEqualTo(((NativeTree) root2).nativeKind());
  }

  private BinaryExpressionTree parseBinary(String code) {
    return (BinaryExpressionTree) parseExpressionOrStatement(code);
  }

  private Tree parseExpressionOrStatement(String code) {
    Tree tree = converter.parse(code);
    return tree.children().get(0).children().get(0);
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
