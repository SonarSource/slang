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

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.CatchTree;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.ExceptionHandlingTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class ExceptionHandlingVisitorTest extends AbstractRubyConverterTest {

  @Test
  void begin_rescue_else_ensure() {
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) rubyStatement("" +
      "begin\n" +
      "  1\n" +
      "  5\n" +
      "rescue A, B, C => a\n" +
      "  2\n" +
      "rescue\n" +
      "  # comment\n" +
      "else\n" +
      "  3\n" +
      "ensure\n" +
      "  4\n" +
      "end");

    assertRange(exceptionHandlingTree.textRange()).hasRange(1, 0, 12, 3);
    assertTree(exceptionHandlingTree.tryBlock()).isBlock(LiteralTree.class, LiteralTree.class);
    assertThat(exceptionHandlingTree.catchBlocks()).hasSize(3);

    CatchTree rescueClause1 = exceptionHandlingTree.catchBlocks().get(0);
    assertRange(rescueClause1.textRange()).hasRange(4, 0, 5, 3);
    assertThat(rescueClause1.keyword().text()).isEqualTo("rescue");
    List<IdentifierTree> exceptionIdentifierTrees = rescueClause1.catchParameter().descendants()
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .collect(Collectors.toList());
    assertThat(exceptionIdentifierTrees).extracting(IdentifierTree::name).containsExactly("A", "B", "C", "a");

    CatchTree rescueClause2 = exceptionHandlingTree.catchBlocks().get(1);
    assertRange(rescueClause2.textRange()).hasRange(6, 0, 8, 0);
    assertThat(rescueClause2.metaData().commentsInside())
      .extracting(Comment::text)
      .containsExactly("# comment");
    assertThat(rescueClause2.catchParameter()).isNull();

    CatchTree elseClause = exceptionHandlingTree.catchBlocks().get(2);
    assertRange(elseClause.textRange()).hasRange(8, 0, 9, 3);
    assertThat(elseClause.keyword().text()).isEqualTo("else");
    assertTree(elseClause.catchBlock()).isLiteral("3");

    assertTree(exceptionHandlingTree.finallyBlock()).isLiteral("4");
  }

  @Test
  void implicit_rescue_not_mapped() {
    Tree tree = rubyStatement("" +
      "def foo\n" +
      "  1\n" +
      "  rescue A\n" +
      "  else\n" +
      "end");
    assertThat(tree.descendants().anyMatch(ExceptionHandlingTree.class::isInstance)).isFalse();
    assertThat(tree.descendants().anyMatch(CatchTree.class::isInstance)).isFalse();

    tree = rubyStatement("" +
      "class A\n" +
      "  rescue\n" +
      "end");
    assertThat(tree.descendants().anyMatch(ExceptionHandlingTree.class::isInstance)).isFalse();
    assertThat(tree.descendants().anyMatch(CatchTree.class::isInstance)).isFalse();

    tree = rubyStatement("" +
      "class A\n" +
      "  ensure\n" +
      "end");
    assertThat(tree.descendants().anyMatch(ExceptionHandlingTree.class::isInstance)).isFalse();
    assertThat(tree.descendants().anyMatch(CatchTree.class::isInstance)).isFalse();

    tree = rubyStatement("" +
      "begin\n" +
      "  def a()\n" +
      "   rescue A\n" +
      "  end\n" +
      "end");
    assertThat(tree.descendants().anyMatch(ExceptionHandlingTree.class::isInstance)).isFalse();
    assertThat(tree.descendants().anyMatch(CatchTree.class::isInstance)).isFalse();
  }

  @Test
  void inline_rescue_not_mapped() {
    Tree tree = rubyStatement("begin; i = raise rescue nil; end;");
    assertThat(tree.descendants().anyMatch(ExceptionHandlingTree.class::isInstance)).isFalse();
    assertThat(tree.descendants().anyMatch(CatchTree.class::isInstance)).isFalse();
  }

  @Test
  void empty_blocks_with_comments() {
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) rubyStatement("" +
      "begin\n" +
      "  # comment 0\n" +
      "  rescue\n" +
      "    # comment 1\n" +
      "  else\n" +
      "    # comment 2\n" +
      "  ensure\n" +
      "    # comment 3\n" +
      "end");

    assertRange(exceptionHandlingTree.textRange()).hasRange(1, 0, 9, 3);
    assertThat(exceptionHandlingTree.catchBlocks()).hasSize(2);

    Tree tryBlock = exceptionHandlingTree.tryBlock();
    assertTree(tryBlock).isBlock();
    assertRange(tryBlock.textRange()).hasRange(1, 0, 3, 2);
    assertThat(tryBlock.metaData().commentsInside())
      .extracting(Comment::text)
      .containsExactly("# comment 0");

    CatchTree rescueClause = exceptionHandlingTree.catchBlocks().get(0);
    assertRange(rescueClause.textRange()).hasRange(3, 2, 5, 2);
    assertThat(rescueClause.metaData().commentsInside())
      .extracting(Comment::text)
      .containsExactly("# comment 1");

    CatchTree elseClause = exceptionHandlingTree.catchBlocks().get(1);
    assertRange(elseClause.textRange()).hasRange(5, 2, 7, 2);
    assertThat(elseClause.metaData().commentsInside())
      .extracting(Comment::text)
      .containsExactly("# comment 2");

    Tree finallyBlock = exceptionHandlingTree.finallyBlock();
    assertRange(finallyBlock.textRange()).hasRange(7, 2, 9, 0);
    assertThat(finallyBlock.metaData().commentsInside())
      .extracting(Comment::text)
      .containsExactly("# comment 3");
  }

  @Test
  void only_ensure_block() {
    ExceptionHandlingTree exceptionHandlingTree = (ExceptionHandlingTree) rubyStatement("" +
      "begin\n" +
      "  ensure\n" +
      "    1\n" +
      "end");

    assertRange(exceptionHandlingTree.textRange()).hasRange(1, 0, 4, 3);
    assertThat(exceptionHandlingTree.catchBlocks()).isEmpty();
    assertTree(exceptionHandlingTree.tryBlock()).isBlock();

    Tree finallyBlock = exceptionHandlingTree.finallyBlock();
    assertTree(finallyBlock).isLiteral("1");
    assertRange(finallyBlock.textRange()).hasRange(3, 4, 3, 5);
  }

}
