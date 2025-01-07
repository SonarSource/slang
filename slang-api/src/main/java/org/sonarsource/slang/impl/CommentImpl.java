/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.impl;

import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextRange;

public class CommentImpl implements Comment {

  private final String text;
  private final String contentText;
  private final TextRange range;
  private final TextRange contentRange;

  public CommentImpl(String text, String contentText, TextRange range, TextRange contentRange) {
    this.contentText = contentText;
    this.text = text;
    this.range = range;
    this.contentRange = contentRange;
  }

  @Override
  public String contentText() {
    return contentText;
  }

  @Override
  public String text() {
    return text;
  }

  @Override
  public TextRange textRange() {
    return range;
  }

  @Override
  public TextRange contentRange() {
    return contentRange;
  }

}
