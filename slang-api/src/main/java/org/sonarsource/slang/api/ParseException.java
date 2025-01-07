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
package org.sonarsource.slang.api;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ParseException extends RuntimeException {

  private final transient TextPointer position;

  public ParseException(String message) {
    this(message, null);
  }

  public ParseException(String message, @Nullable TextPointer position) {
    this(message, position, null);
  }

  public ParseException(String message, @Nullable TextPointer position, @Nullable Throwable cause) {
    super(message, cause);
    this.position = position;
  }

  @CheckForNull
  public TextPointer getPosition() {
    return position;
  }

}
