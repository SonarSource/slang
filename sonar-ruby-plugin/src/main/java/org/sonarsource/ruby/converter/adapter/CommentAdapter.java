/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
import org.sonarsource.analyzer.commons.TokenLocation;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.TextRanges;

public class CommentAdapter extends JRubyObjectAdapter<IRubyObject> {

  public CommentAdapter(Ruby runtime, IRubyObject underlyingRubyObject) {
    super(runtime, underlyingRubyObject);
  }

  public Comment toSlangComment() {
    String text = getFromUnderlying("text", String.class);
    TextRange initialTextRange = getTextRange();

    if (text.startsWith("#")) {
      String contentText = text.substring(1);
      int newStartLineOffset = initialTextRange.start().lineOffset() + 1;
      TextRange contentRange = TextRanges.range(initialTextRange.start().line(), newStartLineOffset, initialTextRange.end().line(), initialTextRange.end().lineOffset());
      return new CommentImpl(text, contentText, initialTextRange, contentRange);

    } else {
      // multi-line comment
      text = text.trim();
      TokenLocation textLocation = new TokenLocation(initialTextRange.start().line(), 0, text);
      TextRange trimmedTextRange = TextRanges.range(textLocation.startLine(), textLocation.startLineOffset(), textLocation.endLine(), textLocation.endLineOffset());

      String contentText = getContentText(text);
      TextRange contentRange = getContentTextRange(text, textLocation, contentText);
      return new CommentImpl(text, contentText, trimmedTextRange, contentRange);
    }
  }

  private static TextRange getContentTextRange(String text, TokenLocation textLocation, String contentText) {
    int contentStartIndex = text.indexOf(contentText);
    String whitespacesContentPrefix = text.substring(0, contentStartIndex);
    TokenLocation prefixLocation = new TokenLocation(textLocation.startLine(), textLocation.startLineOffset(), whitespacesContentPrefix);

    TokenLocation contentLocation = new TokenLocation(prefixLocation.endLine(), prefixLocation.endLineOffset(), contentText);
    return TextRanges.range(contentLocation.startLine(), contentLocation.startLineOffset(), contentLocation.endLine(), contentLocation.endLineOffset());
  }

  private static String getContentText(String text) {
    String contentText = text.substring(6).trim();
    int endIndex = contentText.lastIndexOf("=end");
    contentText = contentText.substring(0, endIndex).trim();
    return contentText;
  }

  private TextRange getTextRange() {
    IRubyObject location = getFromUnderlying("location", IRubyObject.class);
    SourceMapAdapter sourceMapAdapter = new SourceMapAdapter(runtime, location);
    return sourceMapAdapter.getRange().toTextRange();
  }

}
