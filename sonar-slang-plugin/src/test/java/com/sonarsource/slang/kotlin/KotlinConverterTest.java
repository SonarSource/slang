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
import com.sonarsource.slang.api.Comment;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.MatchTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.ParameterTree;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.parser.SLangConverter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.sonarsource.slang.testing.RangeAssert.assertRange;
import static com.sonarsource.slang.testing.TreeAssert.assertTree;
import static com.sonarsource.slang.testing.TreesAssert.assertTrees;
import static org.assertj.core.api.Assertions.assertThat;

public class KotlinConverterTest {

  private KotlinConverter converter = new KotlinConverter();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testParseException() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("Cannot convert file due to syntactic errors");
    converter.parse("enum class A {\n<!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!>}");
  }

  @Test
  public void testBinaryExpression() {
    assertTrees(kotlinStatements("x + 2; x - 2; x * 2; x / 2; x == 2; x != 2; x > 2; x >= 2; x < 2; x <= 2; x && y; x || y;"))
      .isEquivalentTo(slangStatements("x + 2; x - 2; x * 2; x / 2; x == 2; x != 2; x > 2; x >= 2; x < 2; x <= 2; x && y; x || y;"));
  }

  @Test
  public void testUnmappedBinaryExpression() {
    Tree or3 = kotlinStatement("x or 3");
    Tree or3b = kotlinStatement("x or 3");
    Tree and3 = kotlinStatement("x and 3");
    assertTree(or3).isInstanceOf(NativeTree.class);
    assertTree(and3).isInstanceOf(NativeTree.class);
    assertTree(or3).isEquivalentTo(or3b);
    assertTree(or3).isNotEquivalentTo(and3);
  }

  @Test
  public void unaryExpressions() {
    assertTree(kotlinStatement("+1")).isEquivalentTo(kotlinStatement("+1"));
    assertTree(kotlinStatement("+1")).isNotEquivalentTo(kotlinStatement("+2"));
    assertTree(kotlinStatement("+1")).isNotEquivalentTo(kotlinStatement("-1"));
  }

  @Test
  public void isExpressions() {
    assertTree(kotlinStatement("a is b")).isEquivalentTo(kotlinStatement("a is b"));
    assertTree(kotlinStatement("a !is b")).isEquivalentTo(kotlinStatement("a !is b"));
    assertTree(kotlinStatement("a !is b")).isNotEquivalentTo(kotlinStatement("a is b"));
    assertTree(kotlinStatement("a is b")).isNotEquivalentTo(kotlinStatement("a is c"));
  }

  @Test
  public void testNullParameterNames() {
    // In the following case, the '(a, b)' part is not a parameter with a name, but a 'KtDestructuringDeclaration'
    assertTree(kotlinStatement("for ((a, b) in container) {a}"))
      .isNotEquivalentTo(kotlinStatement("for ((b, a) in container) {a}"));
    assertTree(kotlinStatement("for ((a, b) in container) {a}"))
      .isEquivalentTo(kotlinStatement("for ((a, b) in container) {a}"));
  }

  @Test
  public void testFunctionDeclaration() {
    FunctionDeclarationTree functionDeclarationTree = ((FunctionDeclarationTree) kotlin("private fun function1(a: Int, b: String): Boolean { true; }"));
    assertTree(functionDeclarationTree.name()).isIdentifier("function1").hasTextRange(1, 12, 1, 21);
    assertThat(functionDeclarationTree.modifiers()).hasSize(1);
    assertTree(functionDeclarationTree.returnType()).isIdentifier("Boolean");
    assertThat(functionDeclarationTree.formalParameters()).hasSize(2);
    assertTree(functionDeclarationTree).hasParameterNames("a", "b");
    assertTree(functionDeclarationTree.body()).isBlock(LiteralTree.class);

    FunctionDeclarationTree functionWithInternalModifier = (FunctionDeclarationTree) kotlin("internal fun function1(a: Int, c: String): Boolean = true");
    assertTree(functionWithInternalModifier.body()).isNotNull();
    assertThat(functionWithInternalModifier.modifiers()).hasSize(1);
    assertTree(functionWithInternalModifier).hasParameterNames("a", "c");

    FunctionDeclarationTree functionWithPrivate = (FunctionDeclarationTree) kotlin("private fun function2() {}");
    assertThat(functionWithPrivate.formalParameters()).isEmpty();
    Tree privateModifier = functionDeclarationTree.modifiers().get(0);
    assertTree(privateModifier).isNotEquivalentTo(functionWithInternalModifier.modifiers().get(0));
    assertTree(privateModifier).isEquivalentTo(functionWithPrivate.modifiers().get(0));

    FunctionDeclarationTree constructorFunction = ((FunctionDeclarationTree) kotlin("class classC(a: String, b: Int) {}").children().get(0));
    assertTree(constructorFunction.name()).isNull();
    assertThat(constructorFunction.modifiers()).isEmpty();
    assertTree(constructorFunction.returnType()).isNull();
    assertThat(constructorFunction.formalParameters()).hasSize(2);
    assertTree(constructorFunction).hasParameterNames("a", "b");
    assertTree(constructorFunction.body()).isNull();

    FunctionDeclarationTree emptyLambdaFunction = (FunctionDeclarationTree) kotlin("{ }");
    assertTree(emptyLambdaFunction.name()).isNull();
    assertThat(emptyLambdaFunction.modifiers()).isEmpty();
    assertTree(emptyLambdaFunction.returnType()).isNull();
    assertThat(emptyLambdaFunction.formalParameters()).isEmpty();
    assertTree(emptyLambdaFunction.body()).isBlock();

    ParameterTree aIntParam1 = functionDeclarationTree.formalParameters().get(0);
    Tree bStringParam = functionDeclarationTree.formalParameters().get(1);
    Tree aIntParam2 = functionWithInternalModifier.formalParameters().get(0);
    Tree aStringParam = constructorFunction.formalParameters().get(1);
    assertTree(aIntParam1).isNotEquivalentTo(bStringParam);
    assertTree(aIntParam1).isEquivalentTo(aIntParam2);
    assertTree(aIntParam1).isNotEquivalentTo(aStringParam);
    assertTree(aStringParam).isNotEquivalentTo(bStringParam);
    assertTree(aIntParam1).hasTextRange(1, 22, 1, 28);
    assertTree(aIntParam1.identifier()).hasTextRange(1, 22, 1, 23);
  }

  @Test
  public void testExtensionFunction() {
    assertTree(kotlin("fun A.fun1() {}"))
      .isNotEquivalentTo(kotlin("fun B.fun1() {}"));
    assertTree(kotlin("fun A.fun1() {}"))
      .isNotEquivalentTo(kotlin("fun fun1() {}"));
    assertTree(kotlin("fun A.fun1() {}"))
      .isEquivalentTo(kotlin("fun A.fun1() {}"));
    assertTree(kotlin("fun A.fun1() {}"))
      .isNotEquivalentTo(kotlin("class A { fun fun1() {} }"));
  }

  @Test
  public void testLiterals() {
    assertTrees(kotlinStatements("554; true; false; null; \"string\"; 'c';"))
      .isEquivalentTo(slangStatements("554; true; false; null; \"string\"; 'c';"));
  }

  @Test
  public void testSimpleStringLiterals() {
    assertTree(kotlinStatement(createEscapedString('\\'))).isStringLiteral(createEscaped('\\'));
    assertTree(kotlinStatement(createEscapedString('\''))).isStringLiteral(createEscaped('\''));
    assertTree(kotlinStatement(createEscapedString('\"'))).isStringLiteral(createEscaped('\"'));
    assertTree(kotlinStatement(createString(""))).isStringLiteral("");
  }

  @Test
  public void testStringWithIdentifier() {
    assertTree(kotlinStatement("\"identifier ${x}\"")).isInstanceOf(NativeTree.class).hasChildren(NativeTree.class, NativeTree.class);
    assertTree(kotlinStatement("\"identifier ${x}\"")).isEquivalentTo(kotlinStatement("\"identifier ${x}\""));
    assertTree(kotlinStatement("\"identifier ${x}\"")).isNotEquivalentTo(kotlinStatement("\"identifier ${y}\""));
    assertTree(kotlinStatement("\"identifier ${x}\"")).isNotEquivalentTo(kotlinStatement("\"id ${x}\""));
    assertTree(kotlinStatement("\"identifier ${x}\"")).isNotEquivalentTo(kotlinStatement("\"identifier \""));
    assertTree(kotlinStatement("\"identifier ${x}\"").children().get(0)).isNotEquivalentTo(kotlinStatement("\"identifier \""));
  }

  @Test
  public void testStringWithBlock() {
    Tree stringWithBlock = kotlinStatement("\"block ${1 == 1}\"");
    assertTree(stringWithBlock).isInstanceOf(NativeTree.class).hasChildren(NativeTree.class, NativeTree.class);
    Tree blockExpressionContainer = stringWithBlock.children().get(1);
    assertTree(blockExpressionContainer).isInstanceOf(NativeTree.class);
    assertThat(blockExpressionContainer.children()).hasSize(1);
    assertTree(blockExpressionContainer.children().get(0)).isBinaryExpression(BinaryExpressionTree.Operator.EQUAL_TO);

    assertTree(kotlinStatement("\"block ${1 == 1}\"")).isEquivalentTo(kotlinStatement("\"block ${1 == 1}\""));
    assertTree(kotlinStatement("\"block ${1 == 1}\"")).isNotEquivalentTo(kotlinStatement("\"block ${1 == 0}\""));
    assertTree(kotlinStatement("\"block ${1 == 1}\"")).isNotEquivalentTo(kotlinStatement("\"B ${1 == 1}\""));
    assertTree(kotlinStatement("\"block ${1 == 1}\"")).isNotEquivalentTo(kotlinStatement("\"block \""));
    assertTree(kotlinStatement("\"block ${1 == 1}\"").children().get(0)).isNotEquivalentTo(kotlinStatement("\"block \""));
  }

  @Test
  public void testMultilineString() {
    assertTree(kotlinStatement("\"\"\"first\nsecond line\"\"\"")).isStringLiteral("\"\"first\nsecond line\"\"");
  }

  @Test
  public void testRange() {
    FunctionDeclarationTree tree = ((FunctionDeclarationTree) kotlin("fun function1(a: Int, b: String): Boolean\n{ true; }"));
    assertTree(tree).hasTextRange(1, 0, 2, 9);
  }

  @Test
  public void testIfExpressions() {
    assertTrees(kotlinStatements("if (x == 0) { 3; x + 2;}"))
      .isEquivalentTo(slangStatements("if (x == 0) { 3; x + 2;}"));

    assertTrees(kotlinStatements("if (x) 1 else 4"))
      .isEquivalentTo(slangStatements("if (x) 1 else 4"));

    assertTrees(kotlinStatements("if (x) 1 else if (x > 2) 4"))
      .isEquivalentTo(slangStatements("if (x) 1 else if (x > 2) 4;"));

    // In kotlin a null 'then' branch is valid code, so this if will be mapped to a native tree as it is not valid in Slang AST
    NativeTree ifStatementWithNullThenBranch = (NativeTree) kotlinStatement("if (x) else 4");
    assertTrees(Collections.singletonList(ifStatementWithNullThenBranch))
      .isNotEquivalentTo(slangStatements("if (x) { } else 4"));
    assertTree(ifStatementWithNullThenBranch).hasChildren(IdentifierTree.class, LiteralTree.class);

    NativeTree ifStatementWithNullBranches = (NativeTree) kotlinStatement("if (x) else;");
    assertTrees(Collections.singletonList(ifStatementWithNullBranches))
      .isNotEquivalentTo(slangStatements("if (x) { } else { }"));
    assertTree(ifStatementWithNullBranches).hasChildren(IdentifierTree.class);
  }

  @Test
  public void testSimpleMatchExpression() {
    Tree kotlinStatement = kotlinStatement("when (x) { 1 -> true; 1 -> false; 2 -> true; else -> true;}");
    assertTree(kotlinStatement).isInstanceOf(MatchTree.class);
    MatchTree matchTree = (MatchTree) kotlinStatement;
    assertTree(matchTree.expression()).isIdentifier("x");
    List<MatchCaseTree> cases = matchTree.cases();
    assertThat(cases).hasSize(4);
    assertTree(getCondition(cases, 0)).isEquivalentTo(getCondition(cases, 1));
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 2));
    assertThat(getCondition(cases, 3)).isNull();
  }

  @Test
  public void testComplexMatchExpression() {
    MatchTree complexWhen = (MatchTree) kotlinStatement("" +
      "when (x) { isBig() -> 1;1,2 -> x; in 5..10 -> y; !in 10..20 -> z; is String -> x; 1,2 -> y; }");
    List<MatchCaseTree> cases = complexWhen.cases();
    assertThat(cases).hasSize(6);
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 1));
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 2));
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 3));
    assertTree(getCondition(cases, 0)).isNotEquivalentTo(getCondition(cases, 4));
    assertTree(getCondition(cases, 1)).isEquivalentTo(getCondition(cases, 5));

    NativeTree emptyWhen = (NativeTree) kotlinStatement("when {}");
    assertTree(emptyWhen).hasChildren(0);
    assertTree(emptyWhen).isEquivalentTo(kotlinStatement("when {}"));
    assertTree(emptyWhen).isNotEquivalentTo(kotlinStatement("when (x) {}"));
  }

  @Test
  public void testComments() {
    Tree parent = converter.parse("#! Shebang comment\n/** Doc comment \n*/\nfun function1(a: /* Block comment */Int, b: String): Boolean { // EOL comment\n true; }");
    assertTree(parent).isInstanceOf(TopLevelTree.class);
    assertThat(parent.children()).hasSize(1);

    TopLevelTree topLevelTree = (TopLevelTree) parent;
    List<Comment> comments = topLevelTree.allComments();
    assertThat(comments).hasSize(4);
    Comment comment = comments.get(1);
    assertRange(comment.textRange()).hasRange(2, 0, 3, 2);
    assertThat(comment.text()).isEqualTo(" Doc comment \n");
    assertThat(comment.textWithDelimiters()).isEqualTo("/** Doc comment \n*/");

    FunctionDeclarationTree tree = (FunctionDeclarationTree) topLevelTree.declarations().get(0);
    List<Comment> commentsInsideFunction = tree.metaData().commentsInside();
    // Kotlin doc is considered part of the function
    assertThat(commentsInsideFunction).hasSize(3);
    comment = commentsInsideFunction.get(2);
    assertRange(comment.textRange()).hasRange(4, 63, 4, 77);
    assertThat(comment.textWithDelimiters()).isEqualTo("// EOL comment");
  }

  @Test
  public void testLambdas() {
    Tree lambdaWithDestructor = kotlinStatement("{ (a, b) -> a.length < b.length }");
    Tree lambdaWithoutDestructor = kotlinStatement("{ a, b -> a.length < b.length }");
    assertTree(lambdaWithDestructor).hasChildren(NativeTree.class);
    assertTree(lambdaWithoutDestructor).hasChildren(FunctionDeclarationTree.class);

    FunctionDeclarationTree emptyLambda = (FunctionDeclarationTree) kotlinStatement("{ }").children().get(0);
    assertThat(emptyLambda.body()).isNull();
  }

  @Test
  public void testEquivalenceWithComments() {
    assertTrees(kotlinStatements("x + 2; // EOL comment"))
      .isEquivalentTo(slangStatements("x + 2"));
  }

  @Test
  public void testMappedComments() {
    TopLevelTree kotlinTree = (TopLevelTree) converter
      .parse("/** 1st comment */\n// comment 2\nfun function() = /* Block comment */ 3;");
    TopLevelTree slangTree = (TopLevelTree) new SLangConverter()
      .parse("/** 1st comment */\n// comment 2\nvoid fun function() { /* Block comment */ 3; }");

    assertThat(kotlinTree.allComments()).hasSize(3);
    assertThat(kotlinTree.allComments()).isNotEqualTo(slangTree.allComments()); // Kotlin considers the '/**' delimiter as separate comments
    List<String> slangCommentsWithDelimiters = slangTree.allComments().stream().map(Comment::textWithDelimiters).collect(Collectors.toList());
    assertThat(kotlinTree.allComments()).extracting(Comment::textWithDelimiters).isEqualTo(slangCommentsWithDelimiters);
  }

  @Test
  public void testAssignments() {
    assertTrees(kotlinStatements("x = 3\nx -= y + 3\n"))
      .isEquivalentTo(slangStatements("x = 3; x -= y + 3"));
  }

  @Test
  public void testTokens() {
    List<Token> tokens = kotlin("private fun foo() { }").metaData().tokens();
    assertThat(tokens).extracting(Token::text).containsExactly("private", "fun", "foo", "(", ")", "{", "}");
    assertThat(tokens).extracting(Token::isKeyword).containsExactly(true, true, false, false, false, false, false);
  }

  private static String createString(String s) {
    return "\"" + s + "\"";
  }

  private static String createEscaped(char s) {
    return "\\" + s;
  }

  private static String createEscapedString(char s) {
    return createString(createEscaped(s));
  }

  private static Tree getCondition(List<MatchCaseTree> cases, int i) {
    return cases.get(i).expression();
  }

  private List<Tree> slangStatements(String innerCode) {
    Tree tree = new SLangConverter().parse(innerCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    return tree.children();
  }

  private Tree kotlinStatement(String innerCode) {
    List<Tree> kotlinStatements = kotlinStatements(innerCode);
    assertThat(kotlinStatements).hasSize(1);
    return kotlinStatements.get(0);
  }

  private Tree kotlin(String innerCode) {
    Tree tree = converter.parse(innerCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    assertThat(tree.children()).hasSize(1);
    return tree.children().get(0);
  }

  private List<Tree> kotlinStatements(String innerCode) {
    FunctionDeclarationTree functionDeclarationTree = (FunctionDeclarationTree) kotlin("fun function1() { " + innerCode + " }");
    assertThat(functionDeclarationTree.body()).isNotNull();
    return functionDeclarationTree.body().statementOrExpressions();
  }
}
