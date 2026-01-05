/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource SA
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

import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import java.util.Objects;

public class TextRangeImpl implements TextRange {

  private final TextPointer start;
  private final TextPointer end;

  public TextRangeImpl (int startLine, int startLineOffset, int endLine, int endLineOffset) {
    this(new TextPointerImpl(startLine, startLineOffset), new TextPointerImpl(endLine, endLineOffset));
  }

  public TextRangeImpl(TextPointer start, TextPointer end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public TextPointer start() {
    return start;
  }

  @Override
  public TextPointer end() {
    return end;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TextRangeImpl textRange = (TextRangeImpl) o;
    return Objects.equals(start, textRange.start) && Objects.equals(end, textRange.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return "TextRange[" + start.line() + ", " + start.lineOffset() + ", " + end.line() + ", " + end.lineOffset() + ']';
  }
}
