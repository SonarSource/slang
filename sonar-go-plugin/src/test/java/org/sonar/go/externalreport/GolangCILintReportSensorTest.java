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
package org.sonar.go.externalreport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonarsource.slang.testing.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.externalreport.ExternalLinterSensorHelper.REPORT_BASE_PATH;

class GolangCILintReportSensorTest {

  private final List<String> analysisWarnings = new ArrayList<>();

  @BeforeEach
  void setup() {
    analysisWarnings.clear();
  }

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    golangCILintReportSensor().describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of GolangCI-Lint issues");
    assertThat(sensorDescriptor.languages()).containsOnly("go");
  }

  private GolangCILintReportSensor golangCILintReportSensor() {
    return new GolangCILintReportSensor(analysisWarnings::add);
  }

  @Test
  void issues_with_sonarqube() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.golangci-lint.reportPaths", REPORT_BASE_PATH.resolve("golandci-lint-report.xml").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(golangCILintReportSensor(), context);
    assertThat(externalIssues).hasSize(2);

    org.sonar.api.batch.sensor.issue.ExternalIssue first = externalIssues.get(0);
    assertThat(first.type()).isEqualTo(RuleType.BUG);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.ruleKey().repository()).isEqualTo("external_golangci-lint");
    assertThat(first.ruleKey().rule()).isEqualTo("deadcode.bug.major");
    assertThat(first.primaryLocation().message()).isEqualTo("`three` is unused");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.ruleKey().repository()).isEqualTo("external_golangci-lint");
    assertThat(second.ruleKey().rule()).isEqualTo("gosec");
    assertThat(second.primaryLocation().message()).isEqualTo("G402: TLS InsecureSkipVerify set true.");
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("module:main.go");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(4);

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }


  @Test
  void import_check_style_report_same_source_different_key() throws IOException {
    // Check that rules have different key based on the severity
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.golangci-lint.reportPaths", REPORT_BASE_PATH.resolve("checkstyle-different-severity.xml").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(golangCILintReportSensor(), context);
    assertThat(externalIssues).hasSize(6);

    assertThat(externalIssues.get(0).ruleKey().rule()).isEqualTo("source1.bug.major");
    assertThat(externalIssues.get(1).ruleKey().rule()).isEqualTo("source1.code_smell.minor");
    assertThat(externalIssues.get(2).ruleKey().rule()).isEqualTo("source1.code_smell.major");
    assertThat(externalIssues.get(3).ruleKey().rule()).isEqualTo("source1.code_smell.major");
    assertThat(externalIssues.get(4).ruleKey().rule()).isEqualTo("source2.bug.major");
    assertThat(externalIssues.get(5).ruleKey().rule()).isEqualTo("source2.code_smell.major");
  }

}
