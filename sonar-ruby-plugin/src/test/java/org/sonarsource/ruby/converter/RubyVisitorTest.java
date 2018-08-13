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
package org.sonarsource.ruby.converter;


import org.junit.Test;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.sonarsource.slang.testing.TreesAssert.assertTrees;

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
    assertTree(rubyStatement("true")).isEquivalentTo(nativeTree(nativeKind("true"), emptyList()));
    assertTree(rubyStatement("false")).isEquivalentTo(nativeTree(nativeKind("false"), emptyList()));
    assertTree(rubyStatement("nil")).isEquivalentTo(nativeTree(nativeKind("nil"), emptyList()));
  }

  @Test
  public void int_literals() {
    assertTrees(rubyStatements("2; 512; 4\n2431323"))
      .isEquivalentTo(slangStatements("2; 512; 4; 2431323;"));
    assertTree(rubyStatement("2")).isLiteral("2");

    // literal bigger than Long.MAX_VALUE are returned as BigInteger by JRuby
    assertTree(rubyStatement("10000000000000000000")).isLiteral("10000000000000000000");
  }

}
