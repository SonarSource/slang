/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.sonarsource.slang.api.TextRange;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Comparator.naturalOrder;

public class TextRanges {

  private static final Supplier<IllegalArgumentException> MERGE_EXCEPTION_SUPPLIER =
    () -> new IllegalArgumentException("Can't merge 0 ranges");

  private TextRanges() {
  }

  public static TextRange range(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    return new TextRangeImpl(startLine, startLineOffset, endLine, endLineOffset);
  }

  public static TextRange merge(List<TextRange> ranges) {
    return new TextRangeImpl(
      ranges.stream().map(TextRange::start).min(naturalOrder()).orElseThrow(MERGE_EXCEPTION_SUPPLIER),
      ranges.stream().map(TextRange::end).max(naturalOrder()).orElseThrow(MERGE_EXCEPTION_SUPPLIER)
    );
  }

}
