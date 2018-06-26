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
import com.sonarsource.slang.api.HasTextRange;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.TreeMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TreeMetaDataProvider {

  public static final Comparator<HasTextRange> COMPARATOR = Comparator.comparing(e -> e.textRange().start());

  private final List<Comment> sortedComments;
  private final List<Token> sortedTokens;

  public TreeMetaDataProvider(List<Comment> comments, List<Token> tokens) {
    this.sortedComments = new ArrayList<>(comments);
    this.sortedComments.sort(COMPARATOR);
    this.sortedTokens = new ArrayList<>(tokens);
    this.sortedTokens.sort(COMPARATOR);
  }

  public List<Comment> allComments() {
    return sortedComments;
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
      return getElementsInsideRange(sortedComments);
    }

    @Override
    public List<Token> tokens() {
      return getElementsInsideRange(sortedTokens);
    }

    private <T extends HasTextRange> List<T> getElementsInsideRange(List<T> sortedList) {
      List<T> elementsInsideRange = new ArrayList<>();
      HasTextRange key = () -> textRange;
      int index = Collections.binarySearch(sortedList, key, COMPARATOR);
      if (index < 0) {
        index = -index - 1;
      }
      for (int i = index; i < sortedList.size(); i++) {
        T element = sortedList.get(i);
        if (!element.textRange().isInside(textRange)) {
          break;
        }
        elementsInsideRange.add(element);
      }
      return elementsInsideRange;
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
