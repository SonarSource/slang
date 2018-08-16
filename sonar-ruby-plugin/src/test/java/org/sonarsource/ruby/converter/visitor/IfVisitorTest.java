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
package org.sonarsource.ruby.converter.visitor;

import org.junit.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class IfVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void if_statements_without_else() {
    IfTree emptyIfTree = (IfTree) rubyStatement("if 1\n# comment\nend");
    assertTree(emptyIfTree.condition()).isLiteral("1");
    assertTree(emptyIfTree.thenBranch()).isBlock();
    assertThat(emptyIfTree.thenBranch().metaData().tokens())
      .extracting(Token::text)
      .containsExactly("if", "1", "end");
    assertThat(emptyIfTree.thenBranch().metaData().commentsInside()).extracting(Comment::text).containsExactly("# comment");
    assertRange(emptyIfTree.thenBranch().textRange()).hasRange(1, 0, 3, 3);
    assertTree(emptyIfTree.elseBranch()).isNull();
    assertThat(emptyIfTree.ifKeyword().text()).isEqualTo("if");
    assertThat(emptyIfTree.elseKeyword()).isNull();

    IfTree emptyIfThenTree = (IfTree) rubyStatement("if 1 then\nend");
    assertTree(emptyIfThenTree.thenBranch()).isBlock();
    assertThat(emptyIfThenTree.thenBranch().metaData().tokens())
      .extracting(Token::text)
      .containsExactly("if", "1", "then", "end");

    IfTree simpleIfTree = (IfTree) rubyStatement("if 1; 2; end");
    assertTree(simpleIfTree.thenBranch()).isLiteral("2");
    assertRange(simpleIfTree.thenBranch().textRange()).hasRange(1, 6, 1, 7);

    IfTree simpleIfTree2 = (IfTree) rubyStatement("2 if 1");
    assertTree(simpleIfTree2.condition()).isLiteral("1");
    assertTree(simpleIfTree2.thenBranch()).isLiteral("2");
    assertRange(simpleIfTree2.thenBranch().textRange()).hasRange(1, 0, 1, 1);
    assertTree(simpleIfTree2.elseBranch()).isNull();
    assertThat(simpleIfTree2.ifKeyword().text()).isEqualTo("if");
    assertThat(simpleIfTree2.elseKeyword()).isNull();
  }

  @Test
  public void chained_if_statements() {
    IfTree ifElseTree = (IfTree) rubyStatement("if 1\nelse\n# comment in else\nend");
    assertTree(ifElseTree.condition()).isLiteral("1");
    assertTree(ifElseTree.thenBranch()).isBlock();
    assertThat(ifElseTree.thenBranch().metaData().tokens())
      .extracting(Token::text)
      .containsExactly("if", "1");
    assertTree(ifElseTree.elseBranch()).isBlock();
    assertThat(ifElseTree.elseBranch().metaData().tokens())
      .extracting(Token::text)
      .containsExactly("else", "end");
    assertThat(ifElseTree.elseBranch().metaData().commentsInside())
      .extracting(Comment::text)
      .containsExactly("# comment in else");
    assertThat(ifElseTree.ifKeyword().text()).isEqualTo("if");
    assertThat(ifElseTree.elseKeyword().text()).isEqualTo("else");

    IfTree chainedIfTree = (IfTree) rubyStatement("if 1; 2; elsif 3; 4; else; 5; end");
    assertTree(chainedIfTree.condition()).isLiteral("1");
    assertTree(chainedIfTree.thenBranch()).isLiteral("2");
    assertThat(chainedIfTree.elseKeyword().text()).isEqualTo("elsif");

    IfTree nestedIfTree = (IfTree) chainedIfTree.elseBranch();
    assertTree(nestedIfTree.condition()).isLiteral("3");
    assertTree(nestedIfTree.thenBranch()).isLiteral("4");
    assertTree(nestedIfTree.elseBranch()).isLiteral("5");
  }

  @Test
  public void mixed_statements() {
    IfTree mixedIfTree1 = (IfTree) rubyStatement("if 1; 2; else; 3 if 4; end");
    assertThat(mixedIfTree1.elseKeyword().text()).isEqualTo("else");
    assertTree(mixedIfTree1.elseBranch()).isBlock(IfTree.class);

    IfTree mixedIfTree2 = (IfTree) rubyStatement("if 1; 2; else; if 4; 3; end; end");
    assertThat(mixedIfTree2.elseKeyword().text()).isEqualTo("else");
    assertTree(mixedIfTree2.elseBranch()).isBlock(IfTree.class);

    IfTree mixedIfTree3 = (IfTree) rubyStatement("if 1; 2; elsif 4; 3; end");
    assertThat(mixedIfTree3.elseKeyword().text()).isEqualTo("elsif");
    IfTree elseIfTree = (IfTree) mixedIfTree3.elseBranch();
    assertThat(elseIfTree.ifKeyword().text()).isEqualTo("elsif");
  }

  @Test
  public void unless_statements_not_supported() {
    // FIXME find a way to map "unless" statements to slang AST so that it makes sense
    Tree unlessStatement = rubyStatement("unless 1\nend");
    assertThat(unlessStatement).isInstanceOf(NativeTree.class);
  }

  @Test
  public void ternary_statements_are_not_ifs() {
    Tree ternaryStatement = rubyStatement("1 ? 2 : 3");
    assertThat(ternaryStatement).isInstanceOf(NativeTree.class);
  }

}
