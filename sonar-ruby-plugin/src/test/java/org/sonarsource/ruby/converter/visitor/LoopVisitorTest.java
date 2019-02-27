/*
 * SonarSource SLang
 * Copyright (C) 2018-2019 SonarSource SA
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

import org.junit.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.NativeTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class LoopVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void while_loops() {
    LoopTree whileLoop = (LoopTree) rubyStatement("while 1 do\n2 end");
    assertRange(whileLoop.textRange()).hasRange(1, 0, 2, 5);
    assertTree(whileLoop.condition()).isLiteral("1");
    assertTree(whileLoop.body()).isLiteral("2");
    assertThat(whileLoop.kind()).isEqualTo(LoopTree.LoopKind.WHILE);
    assertThat(whileLoop.keyword().text()).isEqualTo("while");

    LoopTree whileLoop1 = (LoopTree) rubyStatement("while 1; 2 end");
    assertRange(whileLoop1.textRange()).hasRange(1, 0, 1, 14);
    assertTree(whileLoop1.condition()).isLiteral("1");
    assertTree(whileLoop1.body()).isLiteral("2");
    assertThat(whileLoop1.kind()).isEqualTo(LoopTree.LoopKind.WHILE);
    assertThat(whileLoop1.keyword().text()).isEqualTo("while");

    LoopTree whileLoop2 = (LoopTree) rubyStatement("while 1 do 1; 1 end");
    assertRange(whileLoop2.textRange()).hasRange(1, 0, 1, 19);
    assertTree(whileLoop2.body()).isBlock(LiteralTree.class, LiteralTree.class);

    LoopTree whileLoop3 = (LoopTree) rubyStatement("while 1; #comment\nend");
    assertTree(whileLoop3.body()).isBlock();
    assertRange(whileLoop3.body().textRange()).hasRange(1, 7, 2, 0);
    assertThat(whileLoop3.body().metaData().commentsInside())
      .extracting(Comment::text)
      .containsExactly("#comment");

    LoopTree whileLoop4 = (LoopTree) rubyStatement("2 while 1"); // This is actually a pre-condition loop
    assertTree(whileLoop4.condition()).isLiteral("1");
    assertTree(whileLoop4.body()).isLiteral("2");
    assertThat(whileLoop4.kind()).isEqualTo(LoopTree.LoopKind.WHILE);
    assertThat(whileLoop4.keyword().text()).isEqualTo("while");

    LoopTree whileLoop5 = (LoopTree) rubyStatement("begin; 2; end while 1");
    assertTree(whileLoop5.condition()).isLiteral("1");
    assertTree(whileLoop5.body()).isBlock(LiteralTree.class);
    assertThat(whileLoop5.kind()).isEqualTo(LoopTree.LoopKind.DOWHILE);
  }

  @Test
  public void until_loops() {
    LoopTree untilLoop = (LoopTree) rubyStatement("until 1 do\n2 end");
    assertRange(untilLoop.textRange()).hasRange(1, 0, 2, 5);
    assertTree(untilLoop.condition()).isLiteral("1");
    assertTree(untilLoop.body()).isLiteral("2");
    assertThat(untilLoop.kind()).isEqualTo(LoopTree.LoopKind.WHILE);
    assertThat(untilLoop.keyword().text()).isEqualTo("until");

    LoopTree untilLoop1 = (LoopTree) rubyStatement("until 1; 2 end");
    assertRange(untilLoop1.textRange()).hasRange(1, 0, 1, 14);
    assertTree(untilLoop1.condition()).isLiteral("1");
    assertTree(untilLoop1.body()).isLiteral("2");
    assertThat(untilLoop1.kind()).isEqualTo(LoopTree.LoopKind.WHILE);
    assertThat(untilLoop1.keyword().text()).isEqualTo("until");

    LoopTree untilLoop2 = (LoopTree) rubyStatement("until 1 do 1; 1 end");
    assertRange(untilLoop2.textRange()).hasRange(1, 0, 1, 19);
    assertTree(untilLoop2.body()).isBlock(LiteralTree.class, LiteralTree.class);

    LoopTree untilLoop3 = (LoopTree) rubyStatement("until 1; #comment\nend");
    assertTree(untilLoop3.body()).isBlock();
    assertRange(untilLoop3.body().textRange()).hasRange(1, 7, 2, 0);
    assertThat(untilLoop3.body().metaData().commentsInside())
      .extracting(Comment::text)
      .containsExactly("#comment");

    LoopTree untilLoop4 = (LoopTree) rubyStatement("2 until 1"); // This is actually a pre-condition loop
    assertTree(untilLoop4.condition()).isLiteral("1");
    assertTree(untilLoop4.body()).isLiteral("2");
    assertThat(untilLoop4.kind()).isEqualTo(LoopTree.LoopKind.WHILE);
    assertThat(untilLoop4.keyword().text()).isEqualTo("until");

    LoopTree untilLoop5 = (LoopTree) rubyStatement("begin; 2; end until 1");
    assertTree(untilLoop5.condition()).isLiteral("1");
    assertTree(untilLoop5.body()).isBlock(LiteralTree.class);
    assertThat(untilLoop5.kind()).isEqualTo(LoopTree.LoopKind.DOWHILE);
  }

  @Test
  public void for_loops() {
    LoopTree forLoop1 = (LoopTree) rubyStatement("for x in [1] do\n 1 end");
    assertRange(forLoop1.textRange()).hasRange(1, 0, 2, 6);
    assertTree(forLoop1.condition()).isInstanceOf(NativeTree.class);
    assertTree(forLoop1.body()).isLiteral("1");
    assertThat(forLoop1.kind()).isEqualTo(LoopTree.LoopKind.FOR);
    assertThat(forLoop1.keyword().text()).isEqualTo("for");

    LoopTree forLoop2 = (LoopTree) rubyStatement("for x, y in [1]; 1 end");
    assertTree(forLoop2.condition()).isInstanceOf(NativeTree.class);
    assertThat(forLoop2.kind()).isEqualTo(LoopTree.LoopKind.FOR);
  }

  @Test
  public void until_while_are_not_equivalent() {
    LoopTree untilLoop = (LoopTree) rubyStatement("2 until 1");

    assertTree(untilLoop).isEquivalentTo(rubyStatement("2 until 1"));
    assertTree(untilLoop).isNotEquivalentTo(rubyStatement("begin; 2; end until 1"));
    assertTree(untilLoop).isNotEquivalentTo(rubyStatement("2 while 1"));
  }

}