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
package org.sonarsource.slang.persistence.conversion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringNativeKindTest {

  @Test
  void constructor() {
    assertThat(new StringNativeKind("ast.Element")).isNotNull();
    assertThat(StringNativeKind.of("ast.Element")).isNotNull();
    assertThat(StringNativeKind.of(null)).isNull();
  }

  @Test
  void to_string() {
    assertThat(new StringNativeKind("ast.Element")).hasToString("ast.Element");
    assertThat(StringNativeKind.of("ast.Element")).hasToString("ast.Element");
    assertThat(StringNativeKind.toString(null)).isNull();
    assertThat(StringNativeKind.toString(new StringNativeKind("ast.Element"))).isEqualTo("ast.Element");
  }

  @Test
  void test_equals() {
    assertThat(new StringNativeKind("ast.Element")).isEqualTo(new StringNativeKind("ast.Element"));
    assertThat(new StringNativeKind("ast.Element")).hasSameHashCodeAs(new StringNativeKind("ast.Element"));
  }
  
}
