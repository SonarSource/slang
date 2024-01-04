/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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
