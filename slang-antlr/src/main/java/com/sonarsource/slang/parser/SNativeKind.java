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
package com.sonarsource.slang.parser;

import com.sonarsource.slang.api.NativeKind;
import java.util.Objects;
import org.antlr.v4.runtime.ParserRuleContext;

public class SNativeKind implements NativeKind {
  private final Class<? extends ParserRuleContext> ctxClass;

  public SNativeKind(ParserRuleContext ctx) {
    ctxClass = ctx.getClass();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    SNativeKind that = (SNativeKind) o;
    return Objects.equals(ctxClass, that.ctxClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ctxClass);
  }
}
