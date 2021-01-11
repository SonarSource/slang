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
package org.sonarsource.ruby.converter;


import org.junit.Test;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class RubyVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void top_level_tree() {
    assertTree(converter.parse(("true\nfalse"))).isInstanceOf(TopLevelTree.class);
    assertTree(converter.parse(("true\r\nfalse"))).isInstanceOf(TopLevelTree.class);
  }

  @Test
  public void parse_with_missing_node() {
    Tree tree = converter.parse("def is_root?\nend"); // method has null argument list
    assertThat(tree).isNotNull();
  }

  @Test
  public void singletons() {
    assertTree(rubyStatement("nil")).isEquivalentTo(nativeTree("nil", "nil"));
  }

}
