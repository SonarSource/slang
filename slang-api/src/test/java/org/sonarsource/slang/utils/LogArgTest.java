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
package org.sonarsource.slang.utils;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class LogArgTest {

  private static Logger LOG = LoggerFactory.getLogger(LogArgTest.class);

  @Test
  void to_string() {
    AtomicInteger counter = new AtomicInteger(42);
    Object arg = LogArg.lazyArg(() -> "counter: " + counter.incrementAndGet());
    assertThat(counter.get()).isEqualTo(42);
    LOG.info("Test {}", arg);
    assertThat(counter.get()).isEqualTo(43);
    assertThat(arg).hasToString("counter: 44");
  }
}
