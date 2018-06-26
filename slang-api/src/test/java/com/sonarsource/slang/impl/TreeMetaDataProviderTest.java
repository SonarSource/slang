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
package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.Comment;
import com.sonarsource.slang.api.Token;
import java.util.Arrays;
import org.junit.Test;

import static com.sonarsource.slang.impl.TextRanges.range;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class TreeMetaDataProviderTest {

  @Test
  public void commentsInside() {
    Comment comment = new CommentImpl("// comment1", "comment1", range(2, 5, 2, 12), range(2, 7, 2, 12));
    TreeMetaDataProvider provider = new TreeMetaDataProvider(singletonList(comment), emptyList());
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 1, 20)).commentsInside()).isEmpty();
    assertThat(provider.metaData(new TextRangeImpl(2, 1, 2, 20)).commentsInside()).containsExactly(comment);
  }

  @Test
  public void tokens() {
    Token token1 = new TokenImpl(new TextRangeImpl(1, 3, 1, 6), "abc", Token.Type.OTHER);
    Token token2 = new TokenImpl(new TextRangeImpl(1, 9, 1, 12), "abc", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 1, 20)).tokens()).containsExactly(token1, token2);
    assertThat(provider.metaData(new TextRangeImpl(1, 3, 1, 8)).tokens()).containsExactly(token1);
  }

  @Test
  public void lines_of_code() {
    Token token1 = new TokenImpl(new TextRangeImpl(1, 3, 1, 6), "abc", Token.Type.OTHER);
    Token token2 = new TokenImpl(new TextRangeImpl(1, 9, 1, 12), "def", Token.Type.OTHER);
    Token token3 = new TokenImpl(new TextRangeImpl(2, 1, 2, 4), "abc", Token.Type.OTHER);
    Token token4 = new TokenImpl(new TextRangeImpl(4, 1, 6, 2), "ab\ncd\nef", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2, token3, token4));
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 1, 20)).linesOfCode()).containsExactly(1);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 2, 20)).linesOfCode()).containsExactly(1, 2);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 3, 20)).linesOfCode()).containsExactly(1, 2);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 6, 20)).linesOfCode()).containsExactly(1, 2, 4, 5, 6);
  }
}
