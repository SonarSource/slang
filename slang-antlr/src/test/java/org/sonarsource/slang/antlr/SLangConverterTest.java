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

package org.sonarsource.slang.antlr;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.ExceptionHandlingTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.JumpTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.impl.ModifierTreeImpl;
import org.sonarsource.slang.parser.SLangConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.api.BinaryExpressionTree.Operator.GREATER_THAN;
import static org.sonarsource.slang.api.LoopTree.LoopKind.DOWHILE;
import static org.sonarsource.slang.api.LoopTree.LoopKind.FOR;
import static org.sonarsource.slang.api.LoopTree.LoopKind.WHILE;
import static org.sonarsource.slang.api.ModifierTree.Kind.PRIVATE;
import static org.sonarsource.slang.api.ModifierTree.Kind.PUBLIC;
import static org.sonarsource.slang.api.Token.Type.KEYWORD;
import static org.sonarsource.slang.api.Token.Type.OTHER;
import static org.sonarsource.slang.api.Token.Type.STRING_LITERAL;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class SLangConverterTest {

  @Rule
  public ExpectedException expected = ExpectedException.none();
  private SLangConverter converter = new SLangConverter();

  @Test
  public void simple_binary_expression() {
    BinaryExpressionTree binary = parseBinary("x + 1;");
    assertTree(binary).isBinaryExpression(Operator.PLUS).hasTextRange(1, 0, 1, 5);
    assertTree(binary.leftOperand()).isIdentifier("x").hasTextRange(1, 0, 1, 1);
    assertTree(binary.rightOperand()).isLiteral("1").hasTextRange(1, 4, 1, 5);
    assertThat(binary.operatorToken().text()).isEqualTo("+");
  }

  @Test
  public void simple_unary_expression() {
    BinaryExpressionTree binary = parseBinary("!!x && !(y && z);");
    Tree left = binary.leftOperand();
    Tree right = binary.rightOperand();

    assertTree(left).isUnaryExpression(UnaryExpressionTree.Operator.NEGATE);
    assertTree(right).isUnaryExpression(UnaryExpressionTree.Operator.NEGATE);

    UnaryExpressionTree unaryLeft = (UnaryExpressionTree) left;
    UnaryExpressionTree unaryRight = (UnaryExpressionTree) right;

    assertTree(unaryLeft.operand()).isUnaryExpression(UnaryExpressionTree.Operator.NEGATE);
    assertTree(unaryRight.operand()).isInstanceOf(ParenthesizedExpressionTree.class);
    ParenthesizedExpressionTree parenthesizedExpression = (ParenthesizedExpressionTree) unaryRight.operand();
    assertTree(parenthesizedExpression.expression()).isBinaryExpression(Operator.CONDITIONAL_AND);
  }

  @Test
  public void parenthesized_expression() {
    BinaryExpressionTree binary = parseBinary("((a && b) && (c || d)) || (y\n|| z);");
    assertTree(binary.leftOperand()).isInstanceOf(ParenthesizedExpressionTree.class);
    assertTree(binary.rightOperand()).isInstanceOf(ParenthesizedExpressionTree.class);
    ParenthesizedExpressionTree left = (ParenthesizedExpressionTree) binary.leftOperand();
    ParenthesizedExpressionTree right = (ParenthesizedExpressionTree) binary.rightOperand();

    assertTree(left).hasTextRange(1, 0, 1, 22);
    assertTree(left.expression()).isBinaryExpression(Operator.CONDITIONAL_AND);
    BinaryExpressionTree innerBinary = (BinaryExpressionTree) left.expression();
    assertTree(innerBinary.leftOperand()).isInstanceOf(ParenthesizedExpressionTree.class);
    assertTree(innerBinary.rightOperand()).isInstanceOf(ParenthesizedExpressionTree.class);
    assertTree(right).hasTextRange(1, 26, 2, 5);
    assertTree(right.expression()).isBinaryExpression(Operator.CONDITIONAL_OR);
  }

  @Test
  public void conditional_and_with_multiple_operands() {
    BinaryExpressionTree binary = parseBinary("x && y && z;");
    assertTree(binary).isBinaryExpression(Operator.CONDITIONAL_AND);
    assertTree(binary.leftOperand()).isIdentifier("x");
    assertTree(binary.rightOperand()).isBinaryExpression(Operator.CONDITIONAL_AND);
    assertRange(binary.operatorToken().textRange()).hasRange(1, 2, 1, 4);
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
    Tree tree = converter.parse("int var x;").children().get(0);
    Tree valueTree = converter.parse("int val x;").children().get(0);
    Tree anotherTree = converter.parse("int var x;").children().get(0);
    Tree yetAnotherTree = converter.parse("boolean var x;").children().get(0);
    assertThat(tree).isInstanceOf(VariableDeclarationTree.class);

    VariableDeclarationTree varDeclX = (VariableDeclarationTree) tree;

    assertThat(varDeclX.children()).hasSize(2);
    assertTree(varDeclX.type()).isIdentifier("int");
    assertTree(varDeclX.identifier()).isIdentifier("x");
    assertTree(varDeclX).isEquivalentTo(anotherTree);
    assertTree(varDeclX).isNotEquivalentTo(yetAnotherTree);
    assertTree(varDeclX).isNotEquivalentTo(valueTree);
  }

  @Test
  public void variable_declaration_with_initializer() {
    Tree tree = converter.parse("int var x = 0;").children().get(0);
    Tree anotherTree = converter.parse("int val x = 0;").children().get(0);
    assertThat(tree).isInstanceOf(VariableDeclarationTree.class);
    assertThat(anotherTree).isInstanceOf(VariableDeclarationTree.class);

    VariableDeclarationTree varDeclX = (VariableDeclarationTree) tree;
    VariableDeclarationTree valDeclX = (VariableDeclarationTree) anotherTree;

    assertThat(varDeclX.children()).hasSize(3);
    assertTree(varDeclX.type()).isIdentifier("int");
    assertTree(varDeclX.identifier()).isIdentifier("x");
    assertTree(varDeclX.initializer()).isLiteral("0");
    assertThat(varDeclX.isVal()).isFalse();
    assertThat(valDeclX.children()).hasSize(3);
    assertTree(valDeclX.type()).isIdentifier("int");
    assertTree(valDeclX.identifier()).isIdentifier("x");
    assertTree(valDeclX.initializer()).isLiteral("0");
    assertThat(valDeclX.isVal()).isTrue();
    assertTree(varDeclX).isEquivalentTo(converter.parse("int var x = 0;").children().get(0));
    assertTree(varDeclX).isNotEquivalentTo(valDeclX);
    assertTree(varDeclX).isNotEquivalentTo(converter.parse("myint var x = 0;").children().get(0));
    assertTree(varDeclX).isNotEquivalentTo(converter.parse("int var x = 1;").children().get(0));
    assertTree(varDeclX).isNotEquivalentTo(converter.parse("var x = 0;").children().get(0));
    assertTree(varDeclX).isNotEquivalentTo(converter.parse("var x;").children().get(0));
  }

  @Test
  public void class_with_identifier_and_body() {
    ClassDeclarationTree classe = parseClass("class MyClass { int val x; fun foo (x); } ");
    assertThat(classe.children()).hasSize(1);
    assertThat(classe.classTree()).isInstanceOf(NativeTree.class);
    assertTree(classe.identifier()).isIdentifier("MyClass");
    NativeTree classChildren = (NativeTree) classe.classTree();
    assertThat(classChildren.children()).hasSize(3);
    assertTree(classChildren.children().get(0)).isIdentifier("MyClass");
    assertThat(classChildren.children().get(1)).isInstanceOf(VariableDeclarationTree.class);
    assertTree(classChildren.children().get(2)).isInstanceOf(FunctionDeclarationTree.class);
  }

  @Test
  public void class_without_body() {
    ClassDeclarationTree classe = parseClass("class MyClass { } ");
    assertThat(classe.children()).hasSize(1);
    assertThat(classe.classTree()).isInstanceOf(NativeTree.class);
    assertTree(classe.identifier()).isIdentifier("MyClass");
    assertRange(classe.identifier().textRange()).hasRange(1, 6, 1, 13);
    NativeTree classChildren = (NativeTree) classe.classTree();
    assertThat(classChildren.children()).hasSize(1);
    assertTree(classChildren.children().get(0)).isIdentifier("MyClass");
  }

  @Test
  public void class_without_identifier() {
    ClassDeclarationTree classe = parseClass("class { int val x; } ");
    assertThat(classe.children()).hasSize(1);
    assertThat(classe.classTree()).isInstanceOf(NativeTree.class);
    assertTree(classe.identifier()).isNull();
    NativeTree classChildren = (NativeTree) classe.classTree();
    assertThat(classChildren.children()).hasSize(1);
    assertThat(classChildren.children().get(0)).isInstanceOf(VariableDeclarationTree.class);
  }

  @Test
  public void class_without_identifier_and_body() {
    ClassDeclarationTree classe = parseClass("class { } ");
    assertThat(classe.children()).hasSize(1);
    assertThat(classe.classTree()).isInstanceOf(NativeTree.class);
    assertTree(classe.identifier()).isNull();
    NativeTree classChildren = (NativeTree) classe.classTree();
    assertThat(classChildren.children()).hasSize(0);
  }

  @Test
  public void nested_class_in_function() {
    FunctionDeclarationTree func = parseFunction("fun foo() { class { } }");
    assertThat(func.children()).hasSize(2);
    assertTree(func.name()).isIdentifier("foo");
    assertTree(func.body()).isBlock(ClassDeclarationTree.class);
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
    assertThat(function.nativeChildren()).isEmpty();

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
    assertTree(privateModifier1).isEquivalentTo(new ModifierTreeImpl(null, PRIVATE));
    assertTree(publicModifier1).isEquivalentTo(new ModifierTreeImpl(null, PUBLIC));

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
    assertTree(ifTree.condition()).isBinaryExpression(GREATER_THAN);
    assertThat(ifTree.elseBranch()).isNull();
    assertThat(ifTree.ifKeyword().text()).isEqualTo("if");
    assertThat(ifTree.elseKeyword()).isNull();
  }

  @Test
  public void if_with_else() {
    Tree tree = converter.parse("if (x > 0) { x == 1; } else { y };").children().get(0);
    assertThat(tree).isInstanceOf(IfTree.class);
    IfTree ifTree = (IfTree) tree;
    assertTree(ifTree).hasTextRange(1, 0, 1, 33);
    assertTree(ifTree.condition()).isBinaryExpression(GREATER_THAN);
    assertTree(ifTree.thenBranch()).isBlock(BinaryExpressionTree.class).hasTextRange(1, 11, 1, 22);
    assertTree(ifTree.elseBranch()).isBlock(IdentifierTree.class);
    assertThat(ifTree.ifKeyword().text()).isEqualTo("if");
    assertThat(ifTree.elseKeyword().text()).isEqualTo("else");
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
  public void for_loop() {
    Tree tree = converter.parse("for (var x = list) { x; };").children().get(0);
    assertTree(tree).isInstanceOf(LoopTree.class).hasTextRange(1, 0, 1, 25);
    LoopTree forLoop = (LoopTree) tree;
    assertThat(forLoop.condition().children()).hasSize(2);
    assertTree(forLoop.body()).isBlock(IdentifierTree.class);
    assertThat(forLoop.kind()).isEqualTo(FOR);
    assertThat(forLoop.keyword().text()).isEqualTo("for");
  }

  @Test
  public void while_loop() {
    Tree tree = converter.parse("while (x > y) { x = x-1; };").children().get(0);
    assertTree(tree).isInstanceOf(LoopTree.class).hasTextRange(1, 0, 1, 26);
    LoopTree forLoop = (LoopTree) tree;
    assertTree(forLoop.condition()).isBinaryExpression(GREATER_THAN);
    assertTree(forLoop.body()).isBlock(AssignmentExpressionTree.class);
    assertThat(forLoop.kind()).isEqualTo(WHILE);
    assertThat(forLoop.keyword().text()).isEqualTo("while");
  }

  @Test
  public void doWhile_loop() {
    Tree tree = converter.parse("do { x = x-1; } while (x > y);").children().get(0);
    assertTree(tree).isInstanceOf(LoopTree.class).hasTextRange(1, 0, 1, 29);
    LoopTree forLoop = (LoopTree) tree;
    assertTree(forLoop.condition()).isBinaryExpression(GREATER_THAN);
    assertTree(forLoop.body()).isBlock(AssignmentExpressionTree.class);
    assertThat(forLoop.kind()).isEqualTo(DOWHILE);
    assertThat(forLoop.keyword().text()).isEqualTo("do");
  }

  @Test
  public void try_catch_finally() {
    Tree tree = converter.parse("try { 1 } catch (e) {} catch () {} finally {};").children().get(0);
    assertTree(tree).isInstanceOf(ExceptionHandlingTree.class).hasTextRange(1, 0, 1, 45);
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
    assertTree(tree).isInstanceOf(ExceptionHandlingTree.class).hasTextRange(1, 0, 1, 22);
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) tree;
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class);
    assertThat(exceptionHandlingTree.catchBlocks()).hasSize(1);
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchParameter()).hasParameterName("e");
    assertTree(exceptionHandlingTree.catchBlocks().get(0).catchBlock()).isBlock();
    assertThat(exceptionHandlingTree.catchBlocks().get(0).keyword().text()).isEqualTo("catch");
    assertTree(exceptionHandlingTree.finallyBlock()).isNull();
  }

  @Test
  public void try_finally() {
    Tree tree = converter.parse("try { 1 } finally {};").children().get(0);
    assertTree(tree).isInstanceOf(ExceptionHandlingTree.class).hasTextRange(1, 0, 1, 20);
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
    assertRange(comment.textRange()).hasRange(1, 17, 1, 31);
    assertRange(comment.contentRange()).hasRange(1, 19, 1, 29);
    assertThat(comment.text()).isEqualTo("/* comment2 */");
    assertThat(comment.contentText()).isEqualTo(" comment2 ");
    assertThat(comments.get(1).contentText()).isEqualTo(" comment3");
    assertRange(comments.get(1).contentRange()).hasRange(1, 37, 1, 46);
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
    Tree tree = converter.parse(slangCode + ";");

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
  public void jump() {
    JumpTree jumpTree = (JumpTree) converter.parse("break foo;").children().get(0);
    assertThat(jumpTree.label().name()).isEqualTo("foo");
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.BREAK);

    jumpTree = (JumpTree) converter.parse("break;").children().get(0);
    assertThat(jumpTree.label()).isNull();
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.BREAK);

    jumpTree = (JumpTree) converter.parse("continue;").children().get(0);
    assertThat(jumpTree.label()).isNull();
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE);

    jumpTree = (JumpTree) converter.parse("continue foo;").children().get(0);
    assertThat(jumpTree.label().name()).isEqualTo("foo");
    assertThat(jumpTree.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE);
  }

  @Test
  public void returnTree() {
    ReturnTree returnTree = (ReturnTree) converter.parse("return true;").children().get(0);
    assertThat(returnTree.body()).isInstanceOf(LiteralTree.class);

    returnTree = (ReturnTree) converter.parse("return;").children().get(0);
    assertThat(returnTree.body()).isNull();
  }

  @Test
  public void tokens() {
    Tree topLevel = converter.parse("if (cond == 42) \"a\";");
    IfTree ifTree = (IfTree) topLevel.children().get(0);
    assertThat(topLevel.metaData().tokens()).extracting(Token::text)
      .containsExactly("if", "(", "cond", "==", "42", ")", "\"a\"", ";");
    assertThat(topLevel.metaData().tokens()).extracting(Token::type)
      .containsExactly(KEYWORD, OTHER, OTHER, OTHER, OTHER, OTHER, STRING_LITERAL, OTHER);
    assertRange(topLevel.metaData().tokens().get(1).textRange()).hasRange(1, 3, 1, 4);
    assertThat(ifTree.condition().metaData().tokens()).extracting(Token::text).containsExactly("cond", "==", "42");
  }

  @Test
  public void methodInvocations() {
    Tree functionInvocationNoArgument = converter.parse("function();");
    assertTree(functionInvocationNoArgument).isEquivalentTo(functionInvocationNoArgument);
    assertTree(functionInvocationNoArgument).isEquivalentTo(converter.parse("function();"));
    assertTree(functionInvocationNoArgument).isNotEquivalentTo(converter.parse("function2();"));
    assertTree(functionInvocationNoArgument).isNotEquivalentTo(converter.parse("function(1);"));
    assertTree(functionInvocationNoArgument).isNotEquivalentTo(converter.parse("function(1, 2);"));
    assertTree(converter.parse("function(1);")).isEquivalentTo(converter.parse("function(1);"));
    assertTree(converter.parse("function(1);")).isNotEquivalentTo(converter.parse("function(1, 2);"));

    assertThat(functionInvocationNoArgument.descendants()
      .anyMatch(e -> e instanceof IdentifierTree && ((IdentifierTree) e).name().equals("function"))).isTrue();
  }

  @Test
  public void parse_failure_1() {
    expected.expect(ParseException.class);
    expected.expectMessage("missing ';' before '<EOF>' at position 1:5");
    converter.parse("x + 1");
  }

  @Test
  public void parse_failure_2() {
    expected.expect(ParseException.class);
    expected.expectMessage("Unexpected parsing error occurred. Last found valid token: 'private' at position 1:0");
    converter.parse("private fun fun foo() {}");
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

  private ClassDeclarationTree parseClass(String code) {
    return (ClassDeclarationTree) converter.parse(code).children().get(0);
  }
}
