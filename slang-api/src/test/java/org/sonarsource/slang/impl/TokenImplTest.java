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

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.Token;

import static org.assertj.core.api.Assertions.assertThat;

class TokenImplTest {
  private static final TokenImpl TOKEN = new TokenImpl(
    new TextRangeImpl(1, 1, 1, 6),
    "hello",
    Token.Type.STRING_LITERAL
  );
  private static final TokenImpl EQUIVALENT = new TokenImpl(
    new TextRangeImpl(1, 1, 1, 6),
    "hello",
    Token.Type.STRING_LITERAL
  );
  private static final TokenImpl OFF_BY_ONE = new TokenImpl(
    new TextRangeImpl(1, 1, 1, 7),
    "hello",
    Token.Type.STRING_LITERAL
  );

  private static final TokenImpl DIFFERENT_TEXT =new TokenImpl(
    new TextRangeImpl(1, 1, 1, 6),
    "world",
    Token.Type.STRING_LITERAL
  );

  private static final TokenImpl DIFFERENT_TYPE = new TokenImpl(
    new TextRangeImpl(1, 1, 1, 6),
    "hello",
    Token.Type.OTHER
  );

  private static final TokenImpl SAME_REFERENCE = TOKEN;
  private static final TokenImpl NULL_REFERENCE = null;

  @Test
  void testEquivalency() {
    assertThat(TOKEN)
      .isEqualTo(SAME_REFERENCE)
      .isEqualTo(EQUIVALENT)
      .isNotEqualTo(NULL_REFERENCE)
      .isNotEqualTo(new Object())
      .isNotEqualTo(OFF_BY_ONE)
      .isNotEqualTo(DIFFERENT_TEXT)
      .isNotEqualTo(DIFFERENT_TYPE);

    assertThat(TOKEN.hashCode())
      .isEqualTo(SAME_REFERENCE.hashCode())
      .isEqualTo(EQUIVALENT.hashCode())
      .isNotEqualTo(new Object().hashCode())
      .isNotEqualTo(OFF_BY_ONE.hashCode())
      .isNotEqualTo(DIFFERENT_TEXT.hashCode())
      .isNotEqualTo(DIFFERENT_TYPE.hashCode());
  }
}