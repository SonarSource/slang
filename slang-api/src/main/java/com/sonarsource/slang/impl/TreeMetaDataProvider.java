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
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.TreeMetaData;
import java.util.List;
import java.util.stream.Collectors;

public class TreeMetaDataProvider {

  private final List<Comment> comments;

  public TreeMetaDataProvider(List<Comment> comments) {
    this.comments = comments;
  }

  public List<Comment> allComments() {
    return comments;
  }

  public TreeMetaData metaData(TextRange textRange, List<Token> tokens) {
    return new TreeMetaDataImpl(textRange, tokens);
  }

  private class TreeMetaDataImpl implements TreeMetaData {

    private final TextRange textRange;
    private final List<Token> tokens;

    private TreeMetaDataImpl(TextRange textRange, List<Token> tokens) {
      this.textRange = textRange;
      this.tokens = tokens;
    }

    @Override
    public TextRange textRange() {
      return textRange;
    }

    @Override
    public List<Comment> commentsInside() {
      // TODO improve performance by storing an ordered list of comments
      return comments.stream()
        .filter(comment -> comment.textRange().isInside(textRange))
        .collect(Collectors.toList());
    }

    @Override
    public List<Token> directTokens() {
      return tokens;
    }
  }
}
