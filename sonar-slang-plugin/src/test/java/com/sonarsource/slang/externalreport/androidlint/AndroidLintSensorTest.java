/*
 * SonarSource SLang
 * Copyright (C) 2009-2018 SonarSource SA
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
package com.sonarsource.slang.externalreport.androidlint;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static com.sonarsource.slang.externalreport.ExternalReportTestUtils.assertLogsContainOnlyInfo;
import static com.sonarsource.slang.externalreport.ExternalReportTestUtils.createContext;
import static com.sonarsource.slang.externalreport.ExternalReportTestUtils.executeSensor;
import static org.assertj.core.api.Assertions.assertThat;

public class AndroidLintSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "androidlint");

  private static AndroidLintSensor detektSensor = new AndroidLintSensor();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    detektSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of Android Lint issues");
    assertThat(sensorDescriptor.languages()).isEmpty();
    assertLogsContainOnlyInfo(logTester);
  }

  @Test
  public void no_issues_with_sonarqube_71() throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, 7, 1);
    context.settings().setProperty("sonar.android.androidLint.reportPaths", resolveInProject("lint-results.xml"));
    List<ExternalIssue> externalIssues = executeSensor(detektSensor, context);
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactly("Import of external issues requires SonarQube 7.2 or greater.");
  }

  @Test
  public void issues_with_sonarqube_72() throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, 7, 2);
    context.settings().setProperty("sonar.android.androidLint.reportPaths", resolveInProject("lint-results.xml"));
    List<ExternalIssue> externalIssues = executeSensor(detektSensor, context);
    assertThat(externalIssues).hasSize(4);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:AndroidManifest.xml");
    assertThat(first.ruleKey().toString()).isEqualTo("android-lint-xml:AllowBackup");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.primaryLocation().message()).isEqualTo(
      "On SDK version 23 and up, your app data will be automatically backed up and restored on app install. Consider adding the attribute `android:fullBackupContent` to specify an `@xml` resource which configures which files to backup. More info: https://developer.android.com/training/backup/autosyncapi.html");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(2);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:A.java");
    assertThat(second.ruleKey().toString()).isEqualTo("android-lint-java:GoogleAppIndexingWarning");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(1);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:B.kt");
    assertThat(third.ruleKey().toString()).isEqualTo("android-lint-kotlin:GoogleAppIndexingWarning");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(2);

    ExternalIssue fourth = externalIssues.get(3);
    assertThat(fourth.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:build.gradle");
    assertThat(fourth.ruleKey().toString()).isEqualTo("android-lint:GradleDependency");
    assertThat(fourth.primaryLocation().textRange().start().line()).isEqualTo(3);

    assertLogsContainOnlyInfo(logTester);
  }

  @Test
  public void no_issues_without_report_paths_property() throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, 7, 2);
    List<ExternalIssue> externalIssues = executeSensor(detektSensor, context);
    assertThat(externalIssues).isEmpty();
    assertLogsContainOnlyInfo(logTester);
  }

  @Test
  public void no_issues_with_invalid_report_path() throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, 7, 2);
    context.settings().setProperty("sonar.android.androidLint.reportPaths", resolveInProject("invalid-path.txt"));
    List<ExternalIssue> externalIssues = executeSensor(detektSensor, context);
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("invalid-path.txt' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_checkstyle_file() throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, 7, 2);
    context.settings().setProperty("sonar.android.androidLint.reportPaths", resolveInProject("not-android-lint-file.xml"));
    List<ExternalIssue> externalIssues = executeSensor(detektSensor, context);
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("not-android-lint-file.xml' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_xml_report() throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, 7, 2);
    context.settings().setProperty("sonar.android.androidLint.reportPaths", resolveInProject("invalid-file.xml"));
    List<ExternalIssue> externalIssues = executeSensor(detektSensor, context);
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("invalid-file.xml' can't be read.");
  }

  @Test
  public void issues_when_xml_file_has_errors() throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, 7, 2);
    context.settings().setProperty("sonar.android.androidLint.reportPaths", resolveInProject("lint-results-with-errors.xml"));
    List<ExternalIssue> externalIssues = executeSensor(detektSensor, context);
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:AndroidManifest.xml");
    assertThat(first.ruleKey().toString()).isEqualTo("android-lint-xml:UnknownRuleKey");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Unknown rule.");
    assertThat(first.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder(
      "No input file found for unknown-file.xml. No android lint issues will be imported on this file."
    );
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  private static String resolveInProject(String fileName) {
    return PROJECT_DIR.resolve(fileName).toAbsolutePath().toString();
  }

}
