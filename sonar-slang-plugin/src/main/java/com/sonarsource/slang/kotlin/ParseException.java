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
package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.api.TextPointer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ParseException extends RuntimeException {

  private final TextPointer position;

  public ParseException(String message, @Nullable TextPointer position) {
    super(message);
    this.position = position;
  }

  public ParseException(String message) {
    super(message);
    this.position = null;
  }

  @CheckForNull
  public TextPointer getPosition() {
    return position;
  }

}
