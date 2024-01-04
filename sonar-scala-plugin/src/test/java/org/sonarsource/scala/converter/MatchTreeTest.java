/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.scala.converter;

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;

import java.lang.annotation.Native;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class MatchTreeTest extends AbstractScalaConverterTest {

  @Test
  void simple_match() {
    MatchTree tree = (MatchTree) scalaStatement("x match { case 1=> a case 2=> b }");
    assertTree(tree).isEquivalentTo(slangStatement("match (x) { 1 -> a; 2 -> b; };"));
    assertThat(tree.keyword().text()).isEqualTo("match");
  }

  @Test
  void default_case() {
    MatchTree tree = (MatchTree) scalaStatement("x match { case 1=> a case _=> b }");
    assertThat(tree.cases().get(1).expression()).isNull();
  }

  @Test
  void case_without_body() {
    MatchTree tree = (MatchTree) scalaStatement("x match { case 1=> a case 2=> /* do nothing */}");
    assertThat(tree.cases().get(1).body()).isNull();
  }

  @Test
  void case_with_condition() {
    Tree tree = scalaStatement(
      "x match { \n"
        + "  case 1 if guard => a\n"
        + "  case 2 => b\n"
        + "  case 3 if (guard2) => c\n"
        + "  case _ if guard3 => d\n"
        + "  case _ :Int => e\n"
        + "  case _ => f\n"
        + "}");
    assertThat(tree).isInstanceOf(MatchTree.class);
    MatchTree matchTree =  (MatchTree) tree;

    List<MatchCaseTree> cases = matchTree.cases();
    assertThat(cases).hasSize(6);

    Tree firstCase = cases.get(0).expression();
    Tree secondCase = cases.get(1).expression();
    Tree thirdCase = cases.get(2).expression();
    Tree defaultCaseWithGuard = cases.get(3).expression();
    Tree defaultCaseWithType = cases.get(4).expression();
    Tree defaultCase = cases.get(5).expression();

    assertThat(firstCase).isInstanceOf(NativeTree.class);
    assertThat(secondCase).isInstanceOf(LiteralTree.class);
    assertThat(thirdCase).isInstanceOf(NativeTree.class);
    assertThat(defaultCaseWithGuard).isInstanceOf(NativeTree.class);
    assertThat(defaultCaseWithType).isInstanceOf(NativeTree.class);
    assertThat(defaultCase).isNull();

    assertThat(firstCase.children()).hasSize(2);
    assertThat(firstCase.metaData().tokens().stream().map(Token::text)).containsExactly("1", "if","guard");

    assertThat(thirdCase.children()).hasSize(2);
    assertThat(thirdCase.children().get(0)).isInstanceOf(LiteralTree.class);
    // skipped parenthesis
    assertThat(thirdCase.children().get(1)).isInstanceOf(IdentifierTree.class);
    assertThat(thirdCase.metaData().tokens().stream().map(Token::text)).containsExactly("3", "if","(", "guard2", ")");

    assertThat(defaultCaseWithGuard.children()).hasSize(2);
    assertThat(defaultCaseWithGuard.metaData().tokens().stream().map(Token::text)).containsExactly("_", "if","guard3");
  }
}
