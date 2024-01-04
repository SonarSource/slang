/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.slang.plugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextRange;

public class CommentAnalysisUtils {
  public static final String NOSONAR_PREFIX = "NOSONAR";

  private static final boolean[] IS_NON_BLANK_CHAR_IN_COMMENTS = new boolean[127];
  static {
    for (int c = 0; c < IS_NON_BLANK_CHAR_IN_COMMENTS.length; c++) {
      IS_NON_BLANK_CHAR_IN_COMMENTS[c] = c > ' ' && "*#-=|".indexOf(c) == -1;
    }
  }

  private CommentAnalysisUtils() { }

  static boolean isNosonarComment(Comment comment) {
    return comment.contentText().trim().toUpperCase(Locale.ENGLISH).startsWith(NOSONAR_PREFIX);
  }

  static Set<Integer> findNonEmptyCommentLines(TextRange range, String content) {
    Set<Integer> lineNumbers = new HashSet<>();

    int startLine = range.start().line();
    if (startLine == range.end().line()) {
      if (isNotBlank(content)) {
        lineNumbers.add(startLine);
      }
    } else {
      String[] lines = content.split("\r\n|\n|\r", -1);
      for (int i = 0; i < lines.length; i++) {
        if (isNotBlank(lines[i])) {
          lineNumbers.add(startLine + i);
        }
      }
    }

    return lineNumbers;
  }

  private static boolean isNotBlank(String line) {
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (ch >= IS_NON_BLANK_CHAR_IN_COMMENTS.length || IS_NON_BLANK_CHAR_IN_COMMENTS[ch]) {
        return true;
      }
    }
    return false;
  }
}
