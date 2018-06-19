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

import com.sonarsource.slang.api.ConditionalKeyword;
import com.sonarsource.slang.api.Token;
import javax.annotation.Nullable;

public class ConditionalKeywordImpl implements ConditionalKeyword {
  private final Token ifKeyword;
  private final Token thenKeyword;
  private final Token elseKeyword;

  public ConditionalKeywordImpl(Token ifKeyword, @Nullable Token thenKeyword, @Nullable Token elseKeyword) {
    this.ifKeyword = ifKeyword;
    this.thenKeyword = thenKeyword;
    this.elseKeyword = elseKeyword;
  }

  @Override
  public Token ifKeyword() {
    return ifKeyword;
  }

  @Override
  public Token thenKeyword() {
    return thenKeyword;
  }

  @Override
  public Token elseKeyword() {
    return elseKeyword;
  }
}
