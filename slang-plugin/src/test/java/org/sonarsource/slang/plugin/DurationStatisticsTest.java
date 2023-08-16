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
package org.sonarsource.slang.plugin;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonarsource.slang.testing.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;

class DurationStatisticsTest {

  private SensorContextTester sensorContext = SensorContextTester.create(Paths.get("."));

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void statistics_disabled() {
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());
    fillStatistics(statistics);
    statistics.log();
    assertThat(logTester.logs(Level.INFO)).isEmpty();
  }

  @Test
  void statistics_activated() {
    sensorContext.settings().setProperty("sonar.slang.duration.statistics", "true");
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());
    fillStatistics(statistics);
    statistics.log();
    assertThat(logTester.logs(Level.INFO)).hasSize(1);
    assertThat(logTester.logs(Level.INFO).get(0)).startsWith("Duration Statistics, ");
  }

  @Test
  void statistics_format() {
    sensorContext.settings().setProperty("sonar.slang.duration.statistics", "true");
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());
    statistics.record("A", 12_000_000L);
    statistics.record("B", 15_000_000_000L);
    statistics.log();
    assertThat(logTester.logs(Level.INFO)).hasSize(1);
    assertThat(logTester.logs(Level.INFO).get(0)).isEqualTo("Duration Statistics, B 15'000 ms, A 12 ms");
  }

  private void fillStatistics(DurationStatistics statistics) {
    StringBuilder txt = new StringBuilder();
    statistics.time("A", () -> txt.append("1")).append(2);
    statistics.time("B", () -> {
      txt.append("3");
    });
    statistics
      .time("C", (t, u) -> txt.append(t).append(u))
      .accept("4", "5");
    assertThat(txt).hasToString("12345");
  }

}
