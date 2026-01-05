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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextPointerImplTest {

  @Test
  void test_equals() {
    TextPointerImpl p1 = new TextPointerImpl(1, 2);
    assertThat(p1)
      .isEqualTo(p1)
      .isEqualTo(new TextPointerImpl(1, 2))
      .isNotEqualTo(new TextPointerImpl(1, 3))
      .isNotEqualTo(new TextPointerImpl(3, 2))
      .isNotEqualTo(null)
      .isNotEqualTo("");
  }

  @Test
  void test_hashCode() {
    assertThat(new TextPointerImpl(1, 2).hashCode()).isEqualTo(new TextPointerImpl(1, 2).hashCode());
    assertThat(new TextPointerImpl(1, 2).hashCode()).isNotEqualTo(new TextPointerImpl(1, 3).hashCode());
  }

  @Test
  void test_compareTo() {
    assertThat(new TextPointerImpl(1, 2)).isEqualByComparingTo(new TextPointerImpl(1, 2));
    assertThat(new TextPointerImpl(1, 2).compareTo(new TextPointerImpl(1, 4))).isEqualTo(-1);
    assertThat(new TextPointerImpl(1, 2).compareTo(new TextPointerImpl(2, 1))).isEqualTo(-1);
    assertThat(new TextPointerImpl(1, 2).compareTo(new TextPointerImpl(1, 1))).isEqualTo(1);
  }
}
