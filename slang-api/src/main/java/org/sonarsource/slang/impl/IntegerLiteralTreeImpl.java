/*
 * SonarSource SLang
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.slang.impl;

import org.sonarsource.slang.api.IntegerLiteralTree;
import org.sonarsource.slang.api.TreeMetaData;

public class IntegerLiteralTreeImpl extends LiteralTreeImpl implements IntegerLiteralTree {

  public IntegerLiteralTreeImpl(TreeMetaData metaData, String value) {
    super(metaData, value);
  }

  @Override
  public boolean isOctal() {
    String value = value();
    if (isExplicitOctal(value)) {
      return true;
    } else if (!"0".equals(value) && value.startsWith("0")) {
      return !isHexadecimal(value) && !isBinary(value) && !isExplicitDecimal(value);
    }
    return false;
  }

  private static boolean isExplicitOctal(String value) {
    return value.startsWith("0o") || value.startsWith("0O");
  }

  private static boolean isExplicitDecimal(String value) {
    return value.startsWith("0d") || value.startsWith("0D");
  }

  private static boolean isHexadecimal(String value) {
    return value.startsWith("0x") || value.startsWith("0X");
  }

  private static boolean isBinary(String value) {
    return value.startsWith("0b") || value.startsWith("0B");
  }

}
