/*
 * SonarSource SLang
 * Copyright (C) 2018-2019 SonarSource SA
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

import java.io.IOException;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.externalreport.ExternalLinterSensorHelper.REPORT_BASE_PATH;

public class GolangCILintReportSensorTest {

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    new GolangCILintReportSensor().describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of GolangCI-Lint issues");
    assertThat(sensorDescriptor.languages()).containsOnly("go");
  }

  @Test
  public void issues_with_sonarqube() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.golangci-lint.reportPaths", REPORT_BASE_PATH.resolve("golandci-lint-report.xml").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(new GolangCILintReportSensor(), context);
    assertThat(externalIssues).hasSize(1);

    org.sonar.api.batch.sensor.issue.ExternalIssue first = externalIssues.get(0);
    assertThat(first.type()).isEqualTo(RuleType.BUG);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.ruleKey().repository()).isEqualTo("external_golangci-lint");
    assertThat(first.ruleKey().rule()).isEqualTo("deadcode");
    assertThat(first.primaryLocation().message()).isEqualTo("`three` is unused");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(3);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

}
