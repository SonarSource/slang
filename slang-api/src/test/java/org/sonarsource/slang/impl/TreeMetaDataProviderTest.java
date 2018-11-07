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
package org.sonarsource.slang.impl;

import java.util.List;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.Token;
import java.util.Arrays;
import org.junit.Test;
import org.sonarsource.slang.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonarsource.slang.impl.TextRanges.range;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class TreeMetaDataProviderTest {

  @Test
  public void commentsInside() {
    Comment comment = new CommentImpl("// comment1", "comment1", range(2, 5, 2, 12), range(2, 7, 2, 12));
    TreeMetaDataProvider provider = new TreeMetaDataProvider(singletonList(comment), emptyList());
    assertThat(provider.allComments()).hasSize(1);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 1, 20)).commentsInside()).isEmpty();
    assertThat(provider.metaData(new TextRangeImpl(2, 1, 2, 20)).commentsInside()).containsExactly(comment);
    assertThat(provider.metaData(new TextRangeImpl(2, 5, 2, 20)).commentsInside()).containsExactly(comment);
  }

  @Test
  public void tokens() {
    Token token1 = new TokenImpl(new TextRangeImpl(1, 3, 1, 6), "abc", Token.Type.OTHER);
    Token token2 = new TokenImpl(new TextRangeImpl(1, 9, 1, 12), "abc", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    assertThat(provider.allTokens()).hasSize(2);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 1, 20)).tokens()).containsExactly(token1, token2);
    assertThat(provider.metaData(new TextRangeImpl(1, 3, 1, 8)).tokens()).containsExactly(token1);
    assertThat(provider.metaData(new TextRangeImpl(1, 3, 1, 6)).tokens()).containsExactly(token1);
  }

  @Test
  public void lines_of_code() {
    Token token1 = new TokenImpl(new TextRangeImpl(1, 3, 1, 6), "abc", Token.Type.OTHER);
    Token token2 = new TokenImpl(new TextRangeImpl(1, 9, 1, 12), "def", Token.Type.OTHER);
    Token token3 = new TokenImpl(new TextRangeImpl(2, 1, 2, 4), "abc", Token.Type.OTHER);
    Token token4 = new TokenImpl(new TextRangeImpl(4, 1, 6, 2), "ab\ncd\nef", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2, token3, token4));
    TreeMetaData metaData = provider.metaData(new TextRangeImpl(1, 1, 1, 20));
    assertThat(metaData.linesOfCode()).containsExactly(1);
    assertThat(metaData.linesOfCode()).containsExactly(1);
    assertThat(metaData.textRange().toString()).isEqualTo("TextRange[1, 1, 1, 20]");
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 2, 20)).linesOfCode()).containsExactly(1, 2);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 3, 20)).linesOfCode()).containsExactly(1, 2);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 6, 20)).linesOfCode()).containsExactly(1, 2, 4, 5, 6);
  }

  @Test
  public void keyword() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    Token token3 = new TokenImpl(range(1, 6, 1, 7), "{",  Token.Type.OTHER);
    Token token4 = new TokenImpl(range(1, 7, 1, 8), "ef", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2, token3, token4));
    assertThat(provider.keyword(range(1, 3, 1, 7))).isEqualTo(token2);
    assertThat(provider.keyword(range(1, 3, 1, 8))).isEqualTo(token2);
    assertThatThrownBy(() -> provider.keyword(range(1, 3, 1, 4)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot find single keyword in TextRange[1, 3, 1, 4]");
    assertThatThrownBy(() -> provider.keyword(range(1, 1, 1, 7)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot find single keyword in TextRange[1, 1, 1, 7]");
  }

  @Test
  public void all_tokens() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    List<Token> allTokens = provider.allTokens();
    assertThat(allTokens).hasSize(2);
    assertThat(allTokens.get(0).text()).isEqualTo("ab");
    assertThat(allTokens.get(1).text()).isEqualTo("cd");
  }

  @Test
  public void index_of_first_token() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    assertThat(provider.indexOfFirstToken(range(1, 0, 1, 1))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 0, 1, 2))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 0, 1, 3))).isEqualTo(0);
    assertThat(provider.indexOfFirstToken(range(1, 1, 1, 3))).isEqualTo(0);
    assertThat(provider.indexOfFirstToken(range(1, 2, 1, 3))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 2, 1, 6))).isEqualTo(1);
    assertThat(provider.indexOfFirstToken(range(1, 4, 1, 6))).isEqualTo(1);
    assertThat(provider.indexOfFirstToken(range(1, 4, 2, 0))).isEqualTo(1);
    assertThat(provider.indexOfFirstToken(range(1, 4, 1, 5))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 5, 1, 10))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 20, 1, 22))).isEqualTo(-1);
  }

  @Test
  public void first_token() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    assertThat(provider.firstToken(range(1, 0, 1, 1)).isPresent()).isFalse();
    assertThat(provider.firstToken(range(1, 1, 1, 3)).get().text()).isEqualTo("ab");
    assertThat(provider.firstToken(range(1, 2, 1, 20)).get().text()).isEqualTo("cd");
    assertThat(provider.firstToken(range(1, 5, 1, 20)).isPresent()).isFalse();
  }

  @Test
  public void previous_token() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    assertThat(provider.previousToken(range(1, 0, 1, 1)).isPresent()).isFalse();
    assertThat(provider.previousToken(range(1, 1, 1, 3)).isPresent()).isFalse();
    assertThat(provider.previousToken(range(1, 2, 1, 20)).get().text()).isEqualTo("ab");
    assertThat(provider.previousToken(range(1, 5, 1, 20)).isPresent()).isFalse();
  }

  @Test
  public void update_token_type() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.OTHER);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    List<Token> allTokens = provider.allTokens();
    assertThat(allTokens).hasSize(2);
    provider.updateTokenType(allTokens.get(0), Token.Type.KEYWORD);
    assertThat(allTokens.get(0).text()).isEqualTo("ab");
    assertThat(allTokens.get(0).type()).isEqualTo(Token.Type.KEYWORD);
    assertThat(allTokens.get(1).text()).isEqualTo("cd");
    assertThat(allTokens.get(1).type()).isEqualTo(Token.Type.OTHER);
  }

  @Test
  public void error_when_updating_token_type() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1));

    Token tokenNotInMetaData1 = new TokenImpl(range(1, 0, 1, 3), "xyz", Token.Type.OTHER);
    assertThatThrownBy(() -> provider.updateTokenType(tokenNotInMetaData1, Token.Type.KEYWORD))
      .hasMessage("token 'xyz' not found in metadata, TextRange[1, 0, 1, 3]");

    Token tokenNotInMetaData2 = new TokenImpl(range(1, 20, 1, 23), "xyz", Token.Type.OTHER);
    assertThatThrownBy(() -> provider.updateTokenType(tokenNotInMetaData2, Token.Type.KEYWORD))
      .hasMessage("token 'xyz' not found in metadata, TextRange[1, 20, 1, 23]");

  }
}
