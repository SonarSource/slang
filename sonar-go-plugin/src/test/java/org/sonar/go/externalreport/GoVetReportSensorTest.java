/*
 * SonarQube Go Plugin
 * Copyright (C) 2018-2021 SonarSource SA
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
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.externalreport.AbstractReportSensor.GENERIC_ISSUE_KEY;
import static org.sonar.go.externalreport.ExternalLinterSensorHelper.REPORT_BASE_PATH;

@EnableRuleMigrationSupport
class GoVetReportSensorTest {

  private final List<String> analysisWarnings = new ArrayList<>();

  @BeforeEach
  void setup() {
    analysisWarnings.clear();
  }

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    goVetReportSensor().describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of go vet issues");
    assertThat(sensorDescriptor.languages()).containsOnly("go");
  }

  @Test
  void issues_with_sonarqube() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.govet.reportPaths", REPORT_BASE_PATH.resolve("govet-report.txt").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(goVetReportSensor(), context);
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.ruleKey().rule()).isEqualTo("nilfunc");
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("comparison of function Foo == nil is always false");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(1);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.ruleKey().rule()).isEqualTo("printf");
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.primaryLocation().message()).isEqualTo("Printf format %s has arg &str of wrong type *string");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(2);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.ruleKey().rule()).isEqualTo("unreachable");
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(third.primaryLocation().message()).isEqualTo("unreachable code");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(2);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  void no_issues_without_govet_property() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(goVetReportSensor(), context);
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  void no_issues_with_invalid_report_path() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.govet.reportPaths", REPORT_BASE_PATH.resolve("invalid-path.txt").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(goVetReportSensor(), context);
    assertThat(externalIssues).isEmpty();
    List<String> warnings = logTester.logs(LoggerLevel.WARN);
    assertThat(warnings)
      .hasSize(1)
      .hasSameSizeAs(analysisWarnings);
    assertThat(warnings.get(0))
      .startsWith("Unable to import go vet report file(s):")
      .contains("invalid-path.txt")
      .endsWith("The report file(s) can not be found. Check that the property 'sonar.go.govet.reportPaths' is correctly configured.");
    assertThat(analysisWarnings.get(0))
      .startsWith("Unable to import 1 go vet report file(s).")
      .endsWith("Please check that property 'sonar.go.govet.reportPaths' is correctly configured and the analysis logs for more details.");
  }

  @Test
  void no_issues_with_invalid_report_line() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.govet.reportPaths", REPORT_BASE_PATH.resolve("govet-report-with-error.txt").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(goVetReportSensor(), context);
    assertThat(externalIssues).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG).get(0)).startsWith("GoVetReportSensor: Unexpected line: abcdefghijkl");
  }

  @Test
  void should_parse_govet_report_line() {
    String line = "./vendor/github.com/foo/go-bar/hello_world.go:550: redundant or: n == 2 || n == 2";
    org.sonar.go.externalreport.ExternalIssue issue = goVetReportSensor().parse(line);
    assertThat(issue).isNotNull();
    assertThat(issue.linter).isEqualTo("govet");
    assertThat(issue.type).isEqualTo(RuleType.BUG);
    assertThat(issue.ruleKey).isEqualTo("bool");
    assertThat(issue.filename).isEqualTo("./vendor/github.com/foo/go-bar/hello_world.go");
    assertThat(issue.lineNumber).isEqualTo(550);
    assertThat(issue.message).isEqualTo("redundant or: n == 2 || n == 2");
  }

  @Test
  void should_match_govet_all_keys() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.govet.reportPaths", REPORT_BASE_PATH.resolve("all-govet-report.txt").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(goVetReportSensor(), context);
    assertThat(externalIssues).hasSize(263);

    Stream<String> uniqueKeys = externalIssues.stream().map(externalIssue -> externalIssue.ruleKey().rule()).distinct();
    assertThat(uniqueKeys).hasSize(19);
    // all messages are associated to a rule key
    assertThat(externalIssues).filteredOn(i -> i.ruleKey().rule().equals(GENERIC_ISSUE_KEY)).isEmpty();
  }

  @Test
  void should_match_govet_asm_keys() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.govet.reportPaths", REPORT_BASE_PATH.resolve("asm-govet-report.txt").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(goVetReportSensor(), context);
    assertThat(externalIssues).hasSize(734);
    // all messages should be matched to asmdecl rule key
    assertThat(externalIssues).extracting(i -> i.ruleKey().rule()).containsOnly("asmdecl");
  }

  @Test
  void should_match_to_generic_issue_if_match_not_found() throws IOException {
    SensorContextTester context = ExternalLinterSensorHelper.createContext();
    context.settings().setProperty("sonar.go.govet.reportPaths", REPORT_BASE_PATH.resolve("govet-with-unknown-message.txt").toString());
    List<ExternalIssue> externalIssues = ExternalLinterSensorHelper.executeSensor(goVetReportSensor(), context);
    assertThat(externalIssues.get(0).ruleKey().rule()).isEqualTo(GENERIC_ISSUE_KEY);
  }

  private GoVetReportSensor goVetReportSensor() {
    return new GoVetReportSensor(analysisWarnings::add);
  }
}
