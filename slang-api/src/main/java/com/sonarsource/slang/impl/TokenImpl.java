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
package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Token;

public class TokenImpl implements Token {

  private final TextRange textRange;
  private final String text;
  private final boolean isKeyword;

  public TokenImpl(TextRange textRange, String text, boolean isKeyword) {
    this.textRange = textRange;
    this.text = text;
    this.isKeyword = isKeyword;
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
  public boolean isKeyword() {
    return isKeyword;
  }
}
