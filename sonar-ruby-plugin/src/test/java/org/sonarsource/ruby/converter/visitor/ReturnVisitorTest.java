/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
package org.sonarsource.ruby.converter.visitor;

import org.junit.jupiter.api.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class ReturnVisitorTest extends AbstractRubyConverterTest {

  @Test
  void return_without_expression() {
    Tree statement = rubyStatement("return");
    assertTree(statement).isEquivalentTo(slangStatement("return;"));
    assertThat(((ReturnTree) statement).keyword().text()).isEqualTo("return");
  }

  @Test
  void return_with_expression() {
    assertTree(rubyStatement("return 42")).isEquivalentTo(slangStatement("return 42;"));
    assertTree(rubyStatement("return 42,43")).isInstanceOf(ReturnTree.class);
    assertThat(rubyStatement("return 42,43").descendants()
      .filter(LiteralTree.class::isInstance)
      .map(l -> ((LiteralTree) l).value()))
      .containsExactly("42", "43");
    assertTree(((ReturnTree) rubyStatement("return 42,43")).body()).hasTextRange(1, 7, 1, 12);
  }

  @Test
  void return_if() {
    Tree returnIf = rubyStatement("return if x");
    assertTree(returnIf).isInstanceOf(IfTree.class);
    assertTree(((IfTree) returnIf).thenBranch()).isInstanceOf(ReturnTree.class);
  }

}
