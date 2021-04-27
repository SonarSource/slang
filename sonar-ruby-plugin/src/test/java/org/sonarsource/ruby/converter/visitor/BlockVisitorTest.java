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
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class BlockVisitorTest extends AbstractRubyConverterTest {

  @Test
  void explicit_begin_block() {
    BlockTree tree = (BlockTree) rubyStatement("" +
      "begin\n" +
      "  1\n" +
      "  begin\n" +
      "    2; 3;\n" +
      "  end\n" +
      "end");

    assertTree(tree).isBlock(LiteralTree.class, BlockTree.class);
    assertRange(tree.textRange()).hasRange(1, 0, 6, 3);

    BlockTree innerBlock = (BlockTree) tree.children().get(1);
    assertRange(innerBlock.textRange()).hasRange(3, 2, 5, 5);
    assertThat(innerBlock.statementOrExpressions()).hasSize(2);
    assertTree(innerBlock.statementOrExpressions().get(0)).isLiteral("2");
    assertTree(innerBlock.statementOrExpressions().get(1)).isLiteral("3");
  }

  @Test
  void implicit_begin_block() {
    Tree beginAsStatementList = rubyStatement("1; b;");
    assertTree(beginAsStatementList).isBlock(LiteralTree.class, NativeTree.class);
    assertTree(beginAsStatementList.children().get(0)).isLiteral("1");
  }

}
