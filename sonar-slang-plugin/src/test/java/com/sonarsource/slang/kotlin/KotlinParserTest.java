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
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.MatchTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.parser.SLangConverter;
import com.sonarsource.slang.visitors.TreePrinter;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.sonarsource.slang.checks.utils.SyntacticEquivalence.areEquivalent;
import static com.sonarsource.slang.kotlin.KotlinParserTest.KotlinTreesAssert.assertTrees;
import static com.sonarsource.slang.testing.RangeAssert.assertRange;
import static com.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.assertj.core.api.Assertions.assertThat;

public class KotlinParserTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testParseException() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("Cannot convert file due to syntactic errors");
    KotlinParser.fromString("enum class A {\n<!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!>}");
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
    assertThat(areEquivalent(or3, or3b)).isTrue();
    assertThat(areEquivalent(or3, and3)).isFalse();
  }

  @Test
  public void unaryExpressions() {
    assertThat(areEquivalent(kotlinStatement("+1"), kotlinStatement("+1"))).isTrue();
    assertThat(areEquivalent(kotlinStatement("+1"), kotlinStatement("+2"))).isFalse();
    assertThat(areEquivalent(kotlinStatement("+1"), kotlinStatement("-1"))).isFalse();
  }

  @Test
  public void isExpressions() {
    assertThat(areEquivalent(kotlinStatement("a is b"), kotlinStatement("a is b"))).isTrue();
    assertThat(areEquivalent(kotlinStatement("a !is b"), kotlinStatement("a !is b"))).isTrue();
    assertThat(areEquivalent(kotlinStatement("a !is b"), kotlinStatement("a is b"))).isFalse();
    assertThat(areEquivalent(kotlinStatement("a is b"), kotlinStatement("a is c"))).isFalse();
  }

  @Test
  public void testFunctionDeclaration() {
    FunctionDeclarationTree functionDeclarationTree = ((FunctionDeclarationTree) kotlin("private fun function1(a: Int, b: String): Boolean { true; }"));
    assertTree(functionDeclarationTree.name()).isIdentifier("function1").hasTextRange(1, 12, 1, 21);
    assertThat(functionDeclarationTree.modifiers()).hasSize(1);
    assertTree(functionDeclarationTree.returnType()).isIdentifier("Boolean");
    assertThat(functionDeclarationTree.formalParameters()).hasSize(2);
    assertTree(functionDeclarationTree.formalParameters().get(1)).isIdentifier("b");
    assertTree(functionDeclarationTree.body()).isBlock(LiteralTree.class);

    FunctionDeclarationTree functionWithInternalModifier = (FunctionDeclarationTree) kotlin("internal fun function1(a: Int, b: String): Boolean = true");
    assertTree(functionWithInternalModifier.body()).isNotNull();
    assertThat(functionWithInternalModifier.modifiers()).hasSize(1);

    FunctionDeclarationTree functionWithPrivate = (FunctionDeclarationTree) kotlin("private fun function2() {}");
    Tree privateModifier1 = functionDeclarationTree.modifiers().get(0);
    Tree internalModifier = functionWithInternalModifier.modifiers().get(0);
    Tree privateModifier2 = functionWithPrivate.modifiers().get(0);
    assertThat(areEquivalent(privateModifier1, internalModifier)).isFalse();
    assertThat(areEquivalent(privateModifier1, privateModifier2)).isTrue();

    FunctionDeclarationTree constructorFunction = ((FunctionDeclarationTree) kotlin("class classC(a: String, b: Int) {}").children().get(0));
    assertTree(constructorFunction.name()).isNull();
    assertThat(constructorFunction.modifiers()).isEmpty();
    assertTree(constructorFunction.returnType()).isNull();
    assertThat(constructorFunction.formalParameters()).hasSize(2);
    assertTree(constructorFunction.body()).isNull();

    FunctionDeclarationTree emptyLambdaFunction = (FunctionDeclarationTree) kotlin("{ }");
    assertTree(emptyLambdaFunction.name()).isNull();
    assertThat(emptyLambdaFunction.modifiers()).isEmpty();
    assertTree(emptyLambdaFunction.returnType()).isNull();
    assertThat(emptyLambdaFunction.formalParameters()).isEmpty();
    assertTree(emptyLambdaFunction.body()).isNull();
  }

  @Test
  public void testLiterals() {
    assertTrees(kotlinStatements("554; true; false; null; \"string\"; 'c';"))
      .isEquivalentTo(slangStatements("554; true; false; null; \"string\"; 'c';"));
  }

  @Test
  public void testSimpleStringLiterals() {
    String escapedBackslash = createEscapedString('\\');
    String escapedSingleQuote = createEscapedString('\'');
    String escapedDoubleQuote = createEscapedString('\"');
    String emptyString = createString("");
    Tree escapedBackslashLiteral = kotlinStatement(escapedBackslash);
    Tree escapedSingleQuoteLiteral = kotlinStatement(escapedSingleQuote);
    Tree escapedDoubleQuoteLiteral = kotlinStatement(escapedDoubleQuote);
    Tree emptyStringLiteral = kotlinStatement(emptyString);
    assertTree(escapedBackslashLiteral).isLiteral(escapedBackslash);
    assertTree(escapedSingleQuoteLiteral).isLiteral(escapedSingleQuote);
    assertTree(escapedDoubleQuoteLiteral).isLiteral(escapedDoubleQuote);
    assertTree(emptyStringLiteral).isLiteral(emptyString);
  }

  @Test
  public void testStringWithIdentifier() {
    Tree stringWithIdentifier = kotlinStatement("\"identifier ${x}\"");
    assertTree(stringWithIdentifier).isInstanceOf(NativeTree.class);
    assertTree(stringWithIdentifier.children().get(0)).isLiteral("identifier ");
    Tree identifierExpressionContainer = stringWithIdentifier.children().get(1);
    assertTree(identifierExpressionContainer).isInstanceOf(NativeTree.class);
    assertThat(identifierExpressionContainer.children()).hasSize(1);
    assertTree(identifierExpressionContainer.children().get(0)).isIdentifier("x");
  }

  @Test
  public void testStringWithBlock() {
    Tree stringWithBlock = kotlinStatement("\"block ${1 == 1}\"");
    assertTree(stringWithBlock).isInstanceOf(NativeTree.class);
    assertTree(stringWithBlock.children().get(0)).isLiteral("block ");
    Tree blockExpressionContainer = stringWithBlock.children().get(1);
    assertTree(blockExpressionContainer).isInstanceOf(NativeTree.class);
    assertThat(blockExpressionContainer.children()).hasSize(1);
    assertTree(blockExpressionContainer.children().get(0)).isBinaryExpression(BinaryExpressionTree.Operator.EQUAL_TO);
  }

  @Test
  public void testMultilineString() {
    String stringValue = "\"\"\"first line\nsecond line\"\"\"";
    Tree multilineString = kotlinStatement(stringValue);
    assertTree(multilineString).isLiteral(stringValue);
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
  }

  @Test
  public void testMatchExpressions() {
    Tree kotlinStatement = kotlinStatement("when (x) { 1 -> true; 1 -> false; 2 -> true; else -> true;}");
    assertTree(kotlinStatement).isInstanceOf(MatchTree.class);
    MatchTree matchTree = (MatchTree) kotlinStatement;
    assertTree(matchTree.expression()).isIdentifier("x");
    List<MatchCaseTree> cases = matchTree.cases();
    assertThat(cases).hasSize(4);
    assertThat(areEquivalent(getCondition(cases, 0), getCondition(cases, 1))).isTrue();
    assertThat(areEquivalent(getCondition(cases, 0), getCondition(cases, 2))).isFalse();
    assertThat(getCondition(cases, 3)).isNull();

    // FIXME check more complex cases when(x) { in 1..10 -> ; 1,2 ->
  }

  @Test
  public void testComments() {
    Tree parent = KotlinParser.fromString("#! Shebang comment\n/** Doc comment \n*/\nfun function1(a: /* Block comment */Int, b: String): Boolean { // EOL comment\n true; }");
    assertTree(parent).isInstanceOf(TopLevelTree.class);
    assertThat(parent.children()).hasSize(3);

    TopLevelTree topLevelTree = (TopLevelTree) parent;
    List<Comment> comments = topLevelTree.allComments();
    assertThat(comments).hasSize(4);
    Comment comment = comments.get(1);
    assertRange(comment.textRange()).hasRange(2, 0, 3, 2);
    assertThat(comment.text()).isEqualTo(" Doc comment \n");
    assertThat(comment.textWithDelimiters()).isEqualTo("/** Doc comment \n*/");

    FunctionDeclarationTree tree = (FunctionDeclarationTree) topLevelTree.declarations().get(2);
    List<Comment> commentsInsideFunction = tree.metaData().commentsInside();
    // Kotlin doc is considered part of the function
    assertThat(commentsInsideFunction).hasSize(3);
    comment = commentsInsideFunction.get(2);
    assertRange(comment.textRange()).hasRange(4, 63, 4, 77);
    assertThat(comment.textWithDelimiters()).isEqualTo("// EOL comment");
  }

  @Test
  public void testEquivalenceWithComments() {
    assertTrees(kotlinStatements("x + 2; // EOL comment"))
      .isEquivalentTo(slangStatements("x + 2"));
  }

  @Test
  public void testMappedComments() {
    TopLevelTree kotlinTree = (TopLevelTree) KotlinParser
      .fromString("/** 1st comment */\n// comment 2\nfun function() = /* Block comment */ 3;");
    TopLevelTree slangTree = (TopLevelTree) new SLangConverter()
      .parse("/** 1st comment */\n// comment 2\nvoid function() { /* Block comment */ 3; }");

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

  private static String createString(String s) {
    return "\"" + s + "\"";
  }

  private static String createEscapedString(char s) {
    return createString("\\" + s);
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
    Tree tree = KotlinParser.fromString(innerCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    assertThat(tree.children()).hasSize(3);
    return tree.children().get(2);
  }

  private List<Tree> kotlinStatements(String innerCode) {
    FunctionDeclarationTree functionDeclarationTree = (FunctionDeclarationTree) kotlin("fun function1() { " + innerCode + " }");
    assertThat(functionDeclarationTree.body()).isNotNull();
    return functionDeclarationTree.body().statementOrExpressions();
  }

  public static class KotlinTreesAssert extends AbstractAssert<KotlinTreesAssert, List<Tree>> {
    public KotlinTreesAssert(List<Tree> actual) {
      super(actual, KotlinTreesAssert.class);
    }

    public KotlinTreesAssert isEquivalentTo(List<Tree> expected) {
      isNotNull();
      boolean equivalent = areEquivalent(actual, expected);
      if (!equivalent) {
        assertThat(TreePrinter.tree2string(actual)).isEqualTo(TreePrinter.tree2string(expected));
        failWithMessage("Expected tree: <%s>\nbut was: <%s>", TreePrinter.tree2string(expected), TreePrinter.tree2string(actual));
      }
      return this;
    }

    public static KotlinTreesAssert assertTrees(List<Tree> actual) {
      return new KotlinTreesAssert(actual);
    }
  }
}
