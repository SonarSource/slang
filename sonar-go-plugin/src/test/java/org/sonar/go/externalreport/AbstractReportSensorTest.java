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
package org.sonar.go.externalreport;

import java.io.File;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonarsource.slang.testing.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AbstractReportSensorTest {

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void report_consumer_logs_io_exception() {
    AbstractReportSensor sensor = new TestReportSensor(mock(AnalysisWarnings.class));
    sensor.reportConsumer(mock(SensorContext.class)).accept(new File("invalid-file.txt"));
    assertThat(logTester.logs(Level.ERROR)).containsExactly("TestReportSensor: No issues information will be saved as the report file 'invalid-file.txt' can't be read.");
  }

  static class TestReportSensor extends AbstractReportSensor {
    TestReportSensor(AnalysisWarnings analysisWarnings) {
      super(analysisWarnings, "propertyKey", "propertyName", "configurationKey");
    }

    @Nullable
    @Override
    ExternalIssue parse(String line) {
      throw new UnsupportedOperationException();
    }
  }
}
