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
package org.sonarsource.ruby.converter.adapter;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.TextRanges;

public class CommentAdapter extends JRubyObjectAdapter<IRubyObject> {

  private static final String MULTILINE_COMMENT_START_TAG = "=begin";
  private static final String MULTILINE_COMMENT_END_TAG = "=end";
  private static final String SINGLE_LINE_COMMENT_PREFIX = "#";

  public CommentAdapter(Ruby runtime, IRubyObject underlyingRubyObject) {
    super(runtime, underlyingRubyObject);
  }

  public Comment toSlangComment() {
    String text = getFromUnderlying("text", String.class);
    IRubyObject location = getFromUnderlying("location", IRubyObject.class);
    SourceMapAdapter sourceMapAdapter = new SourceMapAdapter(runtime, location);
    TextRange textRange = sourceMapAdapter.getRange().toTextRange();
    String contentText = text;
    TextRange contentRange = textRange;

    if (text.startsWith(SINGLE_LINE_COMMENT_PREFIX)) {
      contentText = text.substring(1);
      int newStartLineOffset = textRange.start().lineOffset() + 1;
      contentRange = TextRanges.range(textRange.start().line(), newStartLineOffset, textRange.end().line(), textRange.end().lineOffset());
    } else if (text.startsWith(MULTILINE_COMMENT_START_TAG)) {
      textRange = fixMultilineTextRange(text, textRange);
      String separator = System.lineSeparator();
      int endIndex = text.lastIndexOf(MULTILINE_COMMENT_END_TAG);
      int separatorLength = separator.length();
      contentText = text.substring(MULTILINE_COMMENT_START_TAG.length() + separatorLength, endIndex - separatorLength);
      String[] lines = contentText.split(separator);
      int newEndLineOffset = 0;
      if (lines.length > 0) {
        newEndLineOffset = lines[lines.length - 1].length();
      }
      contentRange = TextRanges.range(
        textRange.start().line() + 1,
        0,
        textRange.end().line() - 1,
        newEndLineOffset);
    }

    return new CommentImpl(text, contentText, textRange, contentRange);
  }

  private static TextRange fixMultilineTextRange(String text, TextRange textRange) {
    String lineSeparator = System.lineSeparator();
    if (text.substring(text.length() - lineSeparator.length()).equals(lineSeparator)) {
      // If =end tag finished with a line separator fix the end position
      return TextRanges.range(
        textRange.start().line(),
        textRange.start().lineOffset(),
        textRange.end().line() - 1,
        MULTILINE_COMMENT_END_TAG.length());
    } else {
      // Fix for comment text range end tag followed by <EOF> character
      return TextRanges.range(
        textRange.start().line(),
        textRange.start().lineOffset(),
        textRange.end().line(),
        textRange.end().lineOffset() - 2);
    }
  }

}
