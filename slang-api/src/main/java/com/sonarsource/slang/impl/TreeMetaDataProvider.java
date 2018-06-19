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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TreeMetaDataProvider {

  private final List<Comment> comments;
  private final List<Token> tokens;

  public TreeMetaDataProvider(List<Comment> comments, List<Token> tokens) {
    this.comments = comments;
    this.tokens = tokens;
  }

  public List<Comment> allComments() {
    return comments;
  }

  public TreeMetaData metaData(TextRange textRange) {
    return new TreeMetaDataImpl(textRange);
  }

  private class TreeMetaDataImpl implements TreeMetaData {

    private final TextRange textRange;
    private Set<Integer> linesOfCode;

    private TreeMetaDataImpl(TextRange textRange) {
      this.textRange = textRange;
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
    public List<Token> tokens() {
      // TODO improve performance by storing an ordered list of tokens
      return tokens.stream()
        .filter(token -> token.textRange().isInside(textRange))
        .collect(Collectors.toList());
    }

    @Override
    public Set<Integer> linesOfCode() {
      if (linesOfCode == null) {
        linesOfCode = computeLinesOfCode();
      }
      return linesOfCode;
    }

    private Set<Integer> computeLinesOfCode() {
      Set<Integer> loc = new HashSet<>();
      for (Token token : tokens()) {
        TextRange range = token.textRange();
        for (int i = range.start().line(); i <= range.end().line(); i++) {
          loc.add(i);
        }
      }
      return loc;
    }

  }

}
