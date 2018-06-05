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

import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.MatchTree;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.parser.SLangConverter;
import com.sonarsource.slang.visitors.TreePrinter;
import java.util.List;
import org.assertj.core.api.AbstractAssert;
import org.junit.Test;

import static com.sonarsource.slang.checks.utils.SyntacticEquivalence.areEquivalent;
import static com.sonarsource.slang.kotlin.KotlinParserTest.KotlinTreesAssert.assertTrees;
import static com.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.assertj.core.api.Assertions.assertThat;

public class KotlinParserTest {

  @Test
  public void testBinaryExpression() {
    assertTrees(kotlinStatements("x + 2; x - 2; x * 2; x / 2; x == 2; x != 2; x > 2; x >= 2; x < 2; x <= 2; x && y; x || y;"))
      .isEquivalentTo(slangStatements("x + 2; x - 2; x * 2; x / 2; x == 2; x != 2; x > 2; x >= 2; x < 2; x <= 2; x && y; x || y;"));
  }

  @Test
  public void testFunctionDeclaration() {
    FunctionDeclarationTree functionDeclarationTree = ((FunctionDeclarationTree) kotlin("fun function1(a: Int, b: String): Boolean { true; }"));
    // FIXME test return type, parameter names, modifiers
    assertTree(functionDeclarationTree.name()).isIdentifier("function1").hasTextRange(1, 4, 1, 13);
    assertThat(functionDeclarationTree.formalParameters()).hasSize(2);
    assertTree(functionDeclarationTree.body()).isBlock(LiteralTree.class);

    assertTree(((FunctionDeclarationTree) kotlin("fun function1(a: Int, b: String): Boolean = true")).body()).isNotNull();
  }

  @Test
  public void testLiterals() {
    assertTrees(kotlinStatements("554; true; false; null; \"string\"; 'c';"))
      .isEquivalentTo(slangStatements("554; true; false; null; \"string\"; 'c';"));
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
