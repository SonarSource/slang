/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import javax.annotation.Nullable;
import org.sonarsource.slang.api.NativeKind;

public class StringNativeKind implements NativeKind {

  private final String kind;

  public StringNativeKind(String kind) {
    this.kind = kind;
  }

  @Nullable
  public static NativeKind of(@Nullable String value) {
    if (value == null) {
      return null;
    }
    return new StringNativeKind(value);
  }

  @Nullable
  public static String toString(@Nullable NativeKind nativeKind) {
    if (nativeKind == null) {
      return null;
    }
    return nativeKind.toString();
  }

  @Override
  public String toString() {
    return kind;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    return kind.equals(((StringNativeKind) other).kind);
  }

  @Override
  public int hashCode() {
    return kind.hashCode();
  }
}
