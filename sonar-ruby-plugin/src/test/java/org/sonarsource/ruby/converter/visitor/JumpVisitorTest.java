/*
 * SonarSource SLang
 * Copyright (C) 2018-2022 SonarSource SA
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
import org.sonarsource.slang.api.NativeTree;

import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class JumpVisitorTest extends AbstractRubyConverterTest {

  @Test
  void without_expression() {
    assertTree(rubyStatement("break")).isEquivalentTo(slangStatement("break;"));
    assertTree(rubyStatement("next")).isEquivalentTo(slangStatement("continue;"));
  }

  @Test
  void with_expression() {
    assertTree(rubyStatement("break x")).isInstanceOf(NativeTree.class);
    assertTree(rubyStatement("next x")).isInstanceOf(NativeTree.class);
    assertTree(rubyStatement("next x")).isEquivalentTo(rubyStatement("next x"));
  }

}
