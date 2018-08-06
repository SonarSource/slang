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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.TextRanges;

public class CommentAdapter extends JRubyObjectAdapter<IRubyObject> {

  private static final Pattern MULTILINE_COMMENTS_PATTERN = Pattern.compile("(=begin\\s*\\r?\\n?)(.*)(=end\\r?\\n?)", Pattern.DOTALL);
  private static final String SINGLE_LINE_COMMENT_PREFIX = "#";
  private static final String MULTILINE_COMMENT_END_TAG = "=end";

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
    } else {
      Matcher matcher = MULTILINE_COMMENTS_PATTERN.matcher(text);
      if (matcher.find()) {
        textRange = fixMultilineTextRange(textRange, matcher.group(3));
        contentText = matcher.group(2);
        contentRange = getContentRange(textRange, matcher.group(1));
      }
    }

    return new CommentImpl(text, contentText, textRange, contentRange);
  }

  private static TextRange fixMultilineTextRange(TextRange textRange, String endTag) {
    if (endTag.contains("\r") || endTag.contains("\n")) {
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

  private static TextRange getContentRange(TextRange textRange, String startTag) {
    int contentRangeStartLine;
    int contentRangeStartLineOffset;
    if (startTag.contains("\r") || startTag.contains("\n")) {
      contentRangeStartLine = textRange.start().line() + 1;
      contentRangeStartLineOffset = 0;
    } else {
      contentRangeStartLine = textRange.start().line();
      contentRangeStartLineOffset = startTag.length();
    }

    return TextRanges.range(
      contentRangeStartLine,
      contentRangeStartLineOffset,
      textRange.end().line(),
      0);
  }

}
