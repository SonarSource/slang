/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
package org.sonarsource.slang.utils;

import java.util.function.Supplier;

/**
 * slf4j does not support lambda argument, so this object wrap the lambda into an object
 * that invoke the lambda when toString is called by the logger.
 */
public class LogArg {

  private final Supplier<String> supplier;

  public LogArg(Supplier<String> supplier) {
    this.supplier = supplier;
  }

  /**
   * wrap a lambda that will only be called by the logger when the toString is called.
   */
  public static Object lazyArg(Supplier<String> supplier) {
    return new LogArg(supplier);
  }

  @Override
  public String toString() {
    return supplier.get();
  }

}
