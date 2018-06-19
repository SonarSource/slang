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
import com.sonarsource.slang.api.ExceptionHandlingTree;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.MatchTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.UnaryExpressionTree;
import com.sonarsource.slang.api.VariableDeclarationTree;
import com.sonarsource.slang.impl.VariableDeclarationTreeImpl;
import com.sonarsource.slang.parser.SLangConverter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

import static com.sonarsource.slang.testing.RangeAssert.assertRange;
import static com.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.assertj.core.api.Assertions.assertThat;

public class SLangConverterTest {

  private SLangConverter converter = new SLangConverter();

  @Test
  public void simple_binary_expression() {
    BinaryExpressionTree binary = parseBinary("x + 1;");
    assertTree(binary).isBinaryExpression(Operator.PLUS).hasTextRange(1, 0, 1, 5);
    assertTree(binary.leftOperand()).isIdentifier("x").hasTextRange(1, 0, 1, 1);
    assertTree(binary.rightOperand()).isLiteral("1").hasTextRange(1, 4, 1, 5);
  }

  @Test
  public void simple_unary_expression() {
    BinaryExpressionTree binary = parseBinary("!(!x) && !(y && z);");
    Tree left = binary.leftOperand();
    Tree right = binary.rightOperand();

    assertTree(left).isUnaryExpression(UnaryExpressionTree.Operator.NEGATE);
    assertTree(right).isUnaryExpression(UnaryExpressionTree.Operator.NEGATE);

    UnaryExpressionTree unaryLeft = (UnaryExpressionTree) left;
    UnaryExpressionTree unaryRight = (UnaryExpressionTree) right;

    assertTree(unaryLeft.operand()).isUnaryExpression(UnaryExpressionTree.Operator.NEGATE);
    assertTree(unaryRight.operand()).isBinaryExpression(Operator.CONDITIONAL_AND);
  }

  @Test
  public void conditional_and_with_multiple_operands() {
    BinaryExpressionTree binary = parseBinary("x && y && z;");
    assertTree(binary).isBinaryExpression(Operator.CONDITIONAL_AND);
    assertTree(binary.leftOperand()).isIdentifier("x");
    assertTree(binary.rightOperand()).isBinaryExpression(Operator.CONDITIONAL_AND);
  }

  @Test
  public void additive_expression_with_multiple_operands() {
    BinaryExpressionTree binary = parseBinary("x + y\n- z;");
    assertTree(binary).isBinaryExpression(Operator.PLUS);
    assertTree(binary.leftOperand()).isIdentifier("x");
    assertTree(binary.rightOperand()).isBinaryExpression(Operator.MINUS).hasTextRange(1, 4, 2, 3);
  }

  @Test
  public void variable_declaration() {
    Tree tree = converter.parse("int x;").children().get(0);
    Tree anotherTree = converter.parse("int x;").children().get(0);
    assertThat(tree).isInstanceOf(VariableDeclarationTree.class);
    assertThat(anotherTree).isInstanceOf(VariableDeclarationTree.class);

    VariableDeclarationTree varDeclX = (VariableDeclarationTree) tree;
    VariableDeclarationTree anotherVarDeclX = (VariableDeclarationTree) anotherTree;

    assertThat(varDeclX.children()).hasSize(1);
    assertTree(varDeclX.identifier()).isIdentifier("x");
    assertTree(varDeclX).isEquivalentTo(anotherVarDeclX);
  }

  @Test
  public void function() {
    FunctionDeclarationTree function = parseFunction("private int fun foo(x1, x2) { x1 + x2 }");
    assertThat(function.name().name()).isEqualTo("foo");
    assertThat(function.modifiers()).hasSize(1);
    assertTree(function.returnType()).isIdentifier("int");
    assertThat(function.formalParameters()).hasSize(2);
    assertTree(function.formalParameters().get(0)).hasParameterName("x1");
    assertThat(function.body()).isNotNull();

    FunctionDeclarationTree publicFunction = parseFunction("public int fun foo(p1);");
    assertThat(publicFunction.formalParameters()).hasSize(1);
    assertTree(publicFunction.formalParameters().get(0)).hasParameterName("p1");

    FunctionDeclarationTree emptyParamFunction = parseFunction("private int fun foo();");
    assertThat(emptyParamFunction.formalParameters()).isEmpty();
    assertThat(emptyParamFunction.body()).isNull();

    Tree privateModifier1 = function.modifiers().get(0);
    Tree publicModifier1 = publicFunction.modifiers().get(0);
    Tree privateModifier2 = emptyParamFunction.modifiers().get(0);
    assertTree(privateModifier1).isNotEquivalentTo(publicModifier1);
    assertTree(privateModifier1).isEquivalentTo(privateModifier2);

    FunctionDeclarationTree simpleFunction = parseFunction("fun foo() {}");
    assertThat(simpleFunction.modifiers()).isEmpty();
    assertThat(simpleFunction.returnType()).isNull();
    assertThat(simpleFunction.body().statementOrExpressions()).isEmpty();

    FunctionDeclarationTree noNameFunction = parseFunction("fun() {}");
    assertThat(noNameFunction.name()).isNull();
  }

  @Test
  public void if_without_else() {
    Tree tree = converter.parse("if (x > 0) { x = 1; };").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree).hasTextRange(1, 0, 1, 21);
    assertTree(ifTree.condition()).isBinaryExpression(Operator.GREATER_THAN);
    assertThat(ifTree.elseBranch()).isNull();
  }

  @Test
  public void if_with_else() {
    Tree tree = converter.parse("if (x > 0) { x == 1; } else { y };").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree).hasTextRange(1, 0, 1, 33);
    assertTree(ifTree.condition()).isBinaryExpression(Operator.GREATER_THAN);
    assertTree(ifTree.thenBranch()).isBlock(BinaryExpressionTree.class).hasTextRange(1, 11, 1, 22);
    assertTree(ifTree.elseBranch()).isBlock(IdentifierTree.class);
  }

  @Test
  public void if_with_else_if() {
    Tree tree = converter.parse("if (x > 0) { x == 1; } else if (x < 1) { y };").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree.elseBranch()).isInstanceOf(IfTree.class);
  }

  @Test
  public void match() {
    Tree tree = converter.parse("match(x) { 1 -> a; else -> b; };").children().get(0);
    assertTree(tree).isInstanceOf(MatchTree.class).hasTextRange(1, 0, 1, 31);
    MatchTree matchTree = (MatchTree) tree;
    assertTree(matchTree.expression()).isIdentifier("x");
    assertThat(matchTree.cases()).hasSize(2);
    assertTree(matchTree.cases().get(0).expression()).isLiteral("1");
    assertTree(matchTree.cases().get(1).expression()).isNull();
    assertTree(matchTree.cases().get(1)).hasTextRange(1, 19, 1, 29);
    assertThat(matchTree.keyword().text()).isEqualTo("match");
  }

  @Test
  public void try_catch_finally() {
    Tree tree = converter.parse("try { 1 } catch (e) {} catch () {} finally {};").children().get(0);
    assertTree(tree).isInstanceOf(ExceptionHandlingTree.class).hasTextRange(1,0,1,45);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) tree;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    assertThat(exceptionHandlingTree.catchBlocks()).hasSize(2);
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchParameter()).hasParameterName("e");
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchBlock()).isBlock();
    assertTree(exceptionHandlingTree.catchBlocks().get(1).catchParameter()).isNull();
    assertTree(exceptionHandlingTree.finallyBlock()).isBlock();
  }

  @Test
  public void try_catch() {
    Tree tree = converter.parse("try { 1 } catch (e) {};").children().get(0);
    assertTree(tree).isInstanceOf(ExceptionHandlingTree.class).hasTextRange(1,0,1,22);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) tree;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    assertThat(exceptionHandlingTree.catchBlocks()).hasSize(1);
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchParameter()).hasParameterName("e");
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchBlock()).isBlock();
    assertTree(exceptionHandlingTree.finallyBlock()).isNull();
  }

  @Test
  public void try_finally() {
    Tree tree = converter.parse("try { 1 } finally {};").children().get(0);
    assertTree(tree).isInstanceOf(ExceptionHandlingTree.class).hasTextRange(1,0,1,20);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) tree;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    assertThat(exceptionHandlingTree.catchBlocks()).hasSize(0);
    assertTree(exceptionHandlingTree.finallyBlock()).isBlock();
  }

  @Test
  public void natives() {
    Tree tree = converter.parse("native [] {};").children().get(0);
    assertTree(tree).isInstanceOf(NativeTree.class).hasTextRange(1, 0, 1, 12);

    tree = converter.parse("native [] { [x] } = x;").children().get(0);
    assertTree(tree).isAssignmentExpression(AssignmentExpressionTree.Operator.EQUAL);
    AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
    assertTree(assignment.leftHandSide()).isInstanceOf(NativeTree.class).hasTextRange(1, 0, 1, 17);
  }

  @Test
  public void simple_assignment() {
    Tree tree = converter.parse("x = 1;").children().get(0);
    assertTree(tree).isAssignmentExpression(AssignmentExpressionTree.Operator.EQUAL).hasTextRange(1, 0, 1, 5);
  }

  @Test
  public void nested_assignments() {
    Tree tree = converter.parse("x -= y += 2;").children().get(0);
    assertTree(tree).isAssignmentExpression(AssignmentExpressionTree.Operator.MINUS_EQUAL).hasTextRange(1, 0, 1, 11);
    AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
    assertTree(assignment.leftHandSide()).isIdentifier("x");
    assertTree(assignment.statementOrExpression()).isAssignmentExpression(AssignmentExpressionTree.Operator.PLUS_EQUAL).hasTextRange(1, 5, 1, 11);
  }

  @Test
  public void top_level_tree() {
    Tree tree1 = converter.parse("int fun foo(p1);\nx == 3;");
    Tree tree2 = converter.parse("x + y\n\n- z;");
    Tree emptyTree = converter.parse("");
    assertTree(tree1)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren(FunctionDeclarationTree.class, BinaryExpressionTree.class)
      .hasTextRange(1, 0, 2, 7);
    assertTree(tree2)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren(BinaryExpressionTree.class)
      .hasTextRange(1, 0, 3, 4);
    assertTree(emptyTree)
      .isInstanceOf(TopLevelTree.class)
      .hasChildren()
      .hasTextRange(1, 0, 1, 0);
  }

  @Test
  public void comments() {
    BinaryExpressionTree binary = parseBinary("/* comment1 */ x /* comment2 */ == // comment3\n1;");
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

  @Test
  public void stringLiterals() {
    List<String> values = Arrays.asList("\"a\"", "\"string\"", "\"string with spaces\"");
    List<String> content = Arrays.asList("a", "string", "string with spaces");

    String slangCode = values.stream().collect(Collectors.joining(";"));
    Tree tree = converter.parse(slangCode+";");

    assertTree(tree).isNotNull();
    assertTree(tree).isInstanceOf(TopLevelTree.class);
    TopLevelTree topLevelTree = (TopLevelTree) tree;
    assertThat(topLevelTree.declarations()).hasSize(3);

    for (int i = 0; i < topLevelTree.declarations().size(); i++) {
      assertTree(topLevelTree.declarations().get(i)).isLiteral(values.get(i));
      assertTree(topLevelTree.declarations().get(i)).isStringLiteral(content.get(i));
    }
  }
  
  @Test
  public void tokens() {
    Tree topLevel = converter.parse("if (cond) x;");
    IfTree ifTree = (IfTree) topLevel.children().get(0);
    assertThat(topLevel.metaData().tokens()).extracting(Token::text).containsExactly("if", "(", "cond", ")", "x", ";");
    assertThat(topLevel.metaData().tokens()).extracting(Token::isKeyword).containsExactly(true, false, false, false, false, false);
    assertRange(topLevel.metaData().tokens().get(1).textRange()).hasRange(1, 3, 1, 4);
    assertThat(ifTree.condition().metaData().tokens()).extracting(Token::text).containsExactly("cond");
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
