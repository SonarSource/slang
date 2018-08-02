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

  private static final String MULTILINE_COMMENT_START_TAG = "=begin\n";
  private static final String MULTILINE_COMMENT_END_TAG = "=end";
  private static final String SINGLE_LINE_COMMENT_PREFIX = "#";

  public CommentAdapter(Ruby runtime, IRubyObject underlyingRubyObject) {
    super(runtime, underlyingRubyObject);
  }

  public String getText() {
    return getFromUnderlying("text", String.class);
  }

  public Comment toSlangComment() {
    String text = getText();
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
      int endIndex = text.lastIndexOf(MULTILINE_COMMENT_END_TAG);
      contentText = text.substring(MULTILINE_COMMENT_START_TAG.length(), endIndex);
      int newEndLineOffset = 0;
      String[] lines = contentText.split("\n");
      if (lines.length > 0) {
        newEndLineOffset = lines[lines.length - 1].length();
      }
      int endLineCorrection = 1;
      if (text.charAt(text.length() - 1) == '\n') {
        // end tag can be "=end" or "=end\n", so we need to correct content range by
        // 2 lines in case closing tag has the new line character in it
        endLineCorrection = 2;
      }
      contentRange = TextRanges.range(
        textRange.start().line() + 1,
        0,
        textRange.end().line() - endLineCorrection,
        newEndLineOffset);
    }

    return new CommentImpl(text, contentText, textRange, contentRange);
  }

}
