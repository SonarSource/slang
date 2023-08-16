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
package org.sonarsource.slang.testing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadLocalLogTesterTest {

  private static final Logger LOG = LoggerFactory.getLogger(ThreadLocalLogTesterTest.class);

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void logs() {
    assertThat(logTester.getLevel()).isEqualTo(Level.DEBUG);

    logTester.setLevel(Level.TRACE);
    assertThat(logTester.getLevel()).isEqualTo(Level.TRACE);
    LOG.trace("BOOM in a {}.", "test");
    assertThat(logTester.logs()).containsExactly("BOOM in a test.");
    logTester.clear();

    logTester.setLevel(Level.DEBUG);
    LOG.error("BOOM in a test.");
    LOG.trace("ignored BOOM in a test.");
    assertThat(logTester.logs()).containsExactly("BOOM in a test.");
    logTester.clear();
    assertThat(logTester.logs()).isEmpty();

    logTester.setLevel(Level.INFO);
    LOG.error("BOOM in {} {}.", "a", "test");
    LOG.error("BOOM {} {} {}.", "in", "a", "test");
    LOG.error("BOOM in a test.", new RuntimeException("BOOM"));
    assertThat(logTester.logs()).containsExactly("BOOM in a test.", "BOOM in a test.", "BOOM in a test.");
    assertThat(logTester.logs(Level.ERROR)).hasSize(3);
    assertThat(logTester.logs(Level.WARN)).isEmpty();
    logTester.clear();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void ignore_logs_from_other_thead() throws InterruptedException {
    LOG.error("BOOM in this thread #1");

    ExecutorService executor = Executors.newFixedThreadPool(2);
    executor.submit(() -> LOG.error("BOOM in other thread #1"));
    LOG.error("BOOM in this thread #2");
    executor.submit(() -> LOG.error("BOOM in other thread #2"));
    executor.shutdown();
    assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();

    LOG.error("BOOM in this thread #3");

    assertThat(logTester.logs()).containsExactly(
      "BOOM in this thread #1",
      "BOOM in this thread #2",
      "BOOM in this thread #3");
  }
}
