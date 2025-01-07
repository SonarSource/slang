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

import java.math.BigInteger;

public interface IntegerLiteralTree extends LiteralTree {

  enum Base {
    BINARY(2),
    OCTAL(8),
    DECIMAL(10),
    HEXADECIMAL(16);

    private int radix;

    Base(int i) {
      radix = i;
    }

    public int getRadix() {
      return radix;
    }
  }

  Base getBase();

  BigInteger getIntegerValue();

  String getNumericPart();

}
