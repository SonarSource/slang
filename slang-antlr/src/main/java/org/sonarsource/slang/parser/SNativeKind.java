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
package org.sonarsource.slang.parser;

import org.sonarsource.slang.api.NativeKind;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;

public class SNativeKind implements NativeKind {
  private final Class<? extends ParserRuleContext> ctxClass;
  private final List<Object> differentiators;

  public SNativeKind(ParserRuleContext ctx) {
    ctxClass = ctx.getClass();
    differentiators = Collections.emptyList();
  }

  public SNativeKind(ParserRuleContext ctx, Object... differentiatorObjs) {
    ctxClass = ctx.getClass();
    differentiators = Arrays.asList(differentiatorObjs);
  }

  public SNativeKind(Class<? extends ParserRuleContext> ctxClass, Object... differentiatorObjs) {
    this.ctxClass = ctxClass;
    differentiators = Arrays.asList(differentiatorObjs);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SNativeKind that = (SNativeKind) o;
    return Objects.equals(ctxClass, that.ctxClass) &&
      Objects.equals(differentiators, that.differentiators);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ctxClass, differentiators);
  }

  @Override
  public String toString() {
    if (differentiators.isEmpty()) {
      return ctxClass.getSimpleName();
    } else {
      return ctxClass.getSimpleName()
        + differentiators.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
    }
  }
}
