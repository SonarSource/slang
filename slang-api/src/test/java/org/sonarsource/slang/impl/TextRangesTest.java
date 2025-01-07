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

import org.sonarsource.slang.api.TextRange;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonarsource.slang.impl.TextRanges.range;
import static org.assertj.core.api.Assertions.assertThat;

class TextRangesTest {

  @Test
  void merge_not_empty_list() {
    assertThat(merge(range(1, 2, 3, 4))).isEqualTo(range(1, 2, 3, 4));
    assertThat(merge(range(1, 2, 3, 4), range(5, 1, 5, 7))).isEqualTo(range(1, 2, 5, 7));
    assertThat(merge(range(1, 2, 3, 4), range(1, 3, 1, 5))).isEqualTo(range(1, 2, 3, 4));
  }

  @Test
  void merge_empty_list() {
    assertThrows(IllegalArgumentException.class,
      TextRangesTest::merge);
  }

  private static TextRange merge(TextRange... ranges) {
    return TextRanges.merge(Arrays.asList(ranges));
  }

}
