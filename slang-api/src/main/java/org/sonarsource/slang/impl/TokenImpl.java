/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource Sàrl
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

import java.util.Objects;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;

public class TokenImpl implements Token {

  private final TextRange textRange;
  private final String text;
  private final Type type;

  public TokenImpl(TextRange textRange, String text, Type type) {
    this.textRange = textRange;
    this.text = text;
    this.type = type;
  }

  @Override
  public TextRange textRange() {
    return textRange;
  }

  @Override
  public String text() {
    return text;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof TokenImpl token) {
      return Objects.equals(textRange, token.textRange) && Objects.equals(text, token.text) && type == token.type;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(textRange, text, type);
  }
}
