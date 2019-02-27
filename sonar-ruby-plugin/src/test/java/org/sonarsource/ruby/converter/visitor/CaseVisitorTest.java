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
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.NativeTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class CaseVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void case_when() {
    MatchTree tree = (MatchTree) rubyStatement("case x\n " +
      "when 42\n " +
      "when 0..4 then doSomething()\n " +
      "when 53 then doSomething(); doSomethingElse()\n " +
      "else doSomething()" +
      "\nend");

    assertThat(tree.keyword().text()).isEqualTo("case");
    assertTree(tree.expression()).isEquivalentTo(sendToIdentifier("x"));
    assertThat(tree.cases()).hasSize(4);
    MatchCaseTree matchCase0 = tree.cases().get(0);
    assertThat(matchCase0.expression()).isNotNull();
    assertTree(matchCase0.body()).isBlock();
    assertTree(matchCase0.body()).hasTextRange(2, 1, 2, 8);
    assertRange(matchCase0.rangeToHighlight()).hasRange(2, 1, 2, 8);

    assertThat(tree.cases().get(1).expression()).isNotNull();
    assertThat(tree.cases().get(1).body()).isInstanceOf(NativeTree.class);
    assertThat(((NativeTree) tree.cases().get(1).body()).nativeKind()).isEqualTo(nativeKind("send"));

    assertThat(tree.cases().get(2).expression()).isNotNull();
    assertTree(tree.cases().get(2).body()).isBlock(NativeTree.class, NativeTree.class);

    MatchCaseTree elseMatchCase = tree.cases().get(3);
    assertThat(elseMatchCase.expression()).isNull();
    assertThat(elseMatchCase.body()).isNotNull();
    assertRange(elseMatchCase.rangeToHighlight()).hasRange(5, 1, 5, 5);
  }

  @Test
  public void case_when_with_empty_else() {
    MatchTree tree = (MatchTree) rubyStatement("case x\n " +
      "when a\n" +
      "else\n" +
      "  # comment" +
      "\nend");

    assertThat(tree.cases()).hasSize(2);
    assertThat(tree.cases().get(0).expression()).isNotNull();
    assertThat(tree.cases().get(0).body()).isNotNull();
    assertThat(tree.cases().get(1).expression()).isNull();
    assertThat(tree.cases().get(1).body()).isNotNull();
    assertThat(tree.cases().get(1).metaData().commentsInside()).extracting(Comment::text).containsExactly("# comment");
    assertThat(tree.cases().get(1).body().metaData().commentsInside()).extracting(Comment::text).containsExactly("# comment");
  }

  @Test
  public void case_when_without_expression_and_else() {
    MatchTree tree = (MatchTree) rubyStatement("case\n when x > 4 then doSomething()\n when x == 3 then doSomething(); doSomethingElse()\nend");

    assertThat(tree.keyword().text()).isEqualTo("case");
    assertThat(tree.expression()).isNull();
    assertThat(tree.cases()).hasSize(2);
    assertThat(tree.cases().get(0).expression()).isNotNull();
    assertThat(tree.cases().get(0).body()).isNotNull();
    assertThat(tree.cases().get(1).expression()).isNotNull();
    assertThat(tree.cases().get(1).body()).isNotNull();
  }

}
