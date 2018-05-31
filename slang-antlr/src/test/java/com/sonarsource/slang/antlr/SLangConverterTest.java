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
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.parser.SLangConverter;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.antlr.v4.runtime.CharStreams;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SLangConverterTest {

  SLangConverter converter = new SLangConverter();

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
    assertThat(numIdentifierNode.get()).isEqualTo(7);
    assertThat(numLiteralNode.get()).isEqualTo(10);
  }

  @Test
  public void simple_binary_expression() {
    BinaryExpressionTree binary = parseBinary("x + 1");
    assertThat(binary.leftOperand()).isInstanceOf(IdentifierTree.class);
    assertThat(binary.rightOperand()).isInstanceOf(LiteralTree.class);
  }

  @Test
  public void conditional_and_with_multiple_operands() {
    BinaryExpressionTree binary = parseBinary("x && y && z");
    assertThat(binary.operator()).isEqualTo(Operator.CONDITIONAL_AND);
    assertThat(binary.leftOperand()).isInstanceOf(IdentifierTree.class);
    assertThat(((IdentifierTree) binary.leftOperand()).name()).isEqualTo("x");
    assertThat(binary.rightOperand()).isInstanceOf(BinaryExpressionTree.class);
  }

  @Test
  public void additive_expression_with_multiple_operands() {
    BinaryExpressionTree binary = parseBinary("x + y - z");
    assertThat(binary.operator()).isEqualTo(Operator.PLUS);
    assertThat(binary.leftOperand()).isInstanceOf(IdentifierTree.class);
    assertThat(((IdentifierTree) binary.leftOperand()).name()).isEqualTo("x");
    assertThat(binary.rightOperand()).isInstanceOf(BinaryExpressionTree.class);
    assertThat(((BinaryExpressionTree) binary.rightOperand()).operator()).isEqualTo(Operator.MINUS);
  }

  @Test
  public void text_ranges() {
    BinaryExpressionTree binary = parseBinary("x + 1");
    assertTextRange(binary.leftOperand(), 1, 0, 1, 1);
    assertTextRange(binary.rightOperand(), 1, 4, 1, 5);
    assertTextRange(binary, 1, 0, 1, 5);

    assertTextRange(converter.parse("42;\n43"), 1, 0, 2, 2);
  }

  private void assertTextRange(Tree leftOperand, int startLine, int startLineOffset, int endLine, int endLineOffset) {
    TextRange leftOperandRange = leftOperand.textRange();
    assertThat(leftOperandRange.start().line()).isEqualTo(startLine);
    assertThat(leftOperandRange.start().lineOffset()).isEqualTo(startLineOffset);
    assertThat(leftOperandRange.end().line()).isEqualTo(endLine);
    assertThat(leftOperandRange.end().lineOffset()).isEqualTo(endLineOffset);
  }

  private BinaryExpressionTree parseBinary(String code) {
    Tree tree = converter.parse(code);
    return (BinaryExpressionTree) tree.children().get(0).children().get(0);
  }
}
