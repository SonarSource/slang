/*
 * SonarSource SLang
 * Copyright (C) 2018-2023 SonarSource SA
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
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.LoopTree.LoopKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class LoopTreeTest extends AbstractScalaConverterTest {

  @Test
  void while_loop() {
    LoopTree tree = (LoopTree) scalaStatement("while(x) { 42 }");
    assertTree(tree).isEquivalentTo(slangStatement("while(x) { 42; };"));
    assertThat(tree.kind()).isEqualTo(LoopKind.WHILE);
    assertThat(tree.keyword().text()).isEqualTo("while");
  }

  @Test
  void do_while() {
    LoopTree tree = (LoopTree) scalaStatement("do { 42 } while (condition)");
    assertTree(tree).isEquivalentTo(slangStatement("do { 42 } while (condition);"));
    assertThat(tree.kind()).isEqualTo(LoopKind.DOWHILE);
    assertThat(tree.keyword().text()).isEqualTo("do");
  }

  @Test
  void for_loop() {
    LoopTree tree = (LoopTree) scalaStatement("for(a <- b) { c }");
    assertThat(tree.kind()).isEqualTo(LoopKind.FOR);
    assertThat(tree.keyword().text()).isEqualTo("for");
    assertThat(identifierDescendants(tree.condition())).containsExactly("a", "b");
    assertThat(identifierDescendants(tree.body())).containsExactly("c");
  }

  @Test
  void for_loop_with_braces() {
    LoopTree tree = (LoopTree) scalaStatement("for{ i <- myList if condition} doSomething");
    assertThat(tree.kind()).isEqualTo(LoopKind.FOR);
    assertThat(identifierDescendants(tree.condition())).containsExactly("i", "myList", "condition");
  }
}
