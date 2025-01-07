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

import java.math.BigInteger;
import org.sonarsource.slang.api.IntegerLiteralTree;
import org.sonarsource.slang.api.TreeMetaData;

/**
 * Languages that can have integer literal with other bases, or who use a different syntax for binary/octal/decimal/hexadecimal
 * values, specific plugins should provide its own implementation of {@link org.sonarsource.slang.api.IntegerLiteralTree}
 */
public class IntegerLiteralTreeImpl extends LiteralTreeImpl implements IntegerLiteralTree {

  private final Base base;
  private final String numericPart;

  public IntegerLiteralTreeImpl(TreeMetaData metaData, String stringValue) {
    super(metaData, stringValue);

    if (hasExplicitHexadecimalPrefix(stringValue)) {
      base = IntegerLiteralTree.Base.HEXADECIMAL;
      numericPart = stringValue.substring(2);
    } else if (hasExplicitBinaryPrefix(stringValue)) {
      base = IntegerLiteralTree.Base.BINARY;
      numericPart = stringValue.substring(2);
    } else if (hasExplicitDecimalPrefix(stringValue)) {
      base = IntegerLiteralTree.Base.DECIMAL;
      numericPart = stringValue.substring(2);
    } else if (hasExplicitOctalPrefix(stringValue)) {
      base = IntegerLiteralTree.Base.OCTAL;
      numericPart = stringValue.substring(2);
    } else if (!stringValue.equals("0") && stringValue.startsWith("0")) {
      base = IntegerLiteralTree.Base.OCTAL;
      numericPart = stringValue.substring(1);
    } else {
      base = Base.DECIMAL;
      numericPart = stringValue;
    }
  }

  @Override
  public Base getBase() {
    return base;
  }

  @Override
  public BigInteger getIntegerValue() {
    return new BigInteger(numericPart, base.getRadix());
  }

  @Override
  public String getNumericPart() {
    return numericPart;
  }

  private static boolean hasExplicitOctalPrefix(String value) {
    return value.startsWith("0o") || value.startsWith("0O");
  }

  private static boolean hasExplicitHexadecimalPrefix(String value) {
    return value.startsWith("0x") || value.startsWith("0X");
  }

  private static boolean hasExplicitBinaryPrefix(String value) {
    return value.startsWith("0b") || value.startsWith("0B");
  }

  private static boolean hasExplicitDecimalPrefix(String value) {
    return value.startsWith("0d") || value.startsWith("0D");
  }

}
