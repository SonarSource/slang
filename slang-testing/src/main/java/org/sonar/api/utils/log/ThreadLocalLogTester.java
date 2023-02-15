/*
 * SonarSource SLang
 * Copyright (C) 2018-2023 SonarSource SA
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
package org.sonar.api.utils.log;

import java.util.List;
import org.junit.rules.ExternalResource;

public class ThreadLocalLogTester extends ExternalResource {

  private static final ListInterceptor INTERCEPTOR = new ListInterceptor();

  @Override
  protected void before() {
    LogInterceptors.set(INTERCEPTOR);
    setLevel(LoggerLevel.INFO);
    INTERCEPTOR.clear();
  }

  @Override
  protected void after() {
    INTERCEPTOR.clear();
    setLevel(LoggerLevel.INFO);
  }

  LoggerLevel getLevel() {
    return Loggers.getFactory().getLevel();
  }

  /**
   * Enable/disable debug logs. Info, warn and error logs are always enabled.
   * By default INFO logs are enabled when LogTester is started.
   */
  public ThreadLocalLogTester setLevel(LoggerLevel level) {
    Loggers.getFactory().setLevel(level);
    return this;
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one)
   */
  public List<String> logs() {
    return INTERCEPTOR.logs();
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one) for
   * a given level
   */
  public List<String> logs(LoggerLevel level) {
    return INTERCEPTOR.logs(level);
  }

  public ThreadLocalLogTester clear() {
    INTERCEPTOR.clear();
    return this;
  }

}
