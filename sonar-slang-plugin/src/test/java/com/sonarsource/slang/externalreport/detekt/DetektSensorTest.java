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
package com.sonarsource.slang.externalreport.detekt;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static com.sonarsource.slang.externalreport.ExternalReportTestUtils.assertNoErrorWarnDebugLogs;
import static com.sonarsource.slang.externalreport.ExternalReportTestUtils.createContext;
import static com.sonarsource.slang.externalreport.ExternalReportTestUtils.onlyOneLogElement;
import static org.assertj.core.api.Assertions.assertThat;

public class DetektSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "detekt");

  private static DetektSensor detektSensor = new DetektSensor();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    detektSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of detekt issues");
    assertThat(sensorDescriptor.languages()).containsOnly("kotlin");
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_with_sonarqube_71() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 1, "detekt-checkstyle.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactly("Import of external issues requires SonarQube 7.2 or greater.");
  }

  @Test
  public void issues_with_sonarqube_72() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "detekt-checkstyle.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("detekt-project:main.kt");
    assertThat(first.ruleKey().rule()).isEqualTo("EmptyIfBlock");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.primaryLocation().message()).isEqualTo("This empty block of code can be removed.");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("detekt-project:main.kt");
    assertThat(second.ruleKey().rule()).isEqualTo("MagicNumber");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.INFO);
    assertThat(second.remediationEffort().longValue()).isEqualTo(10L);
    assertThat(second.primaryLocation().message()).isEqualTo("This expression contains a magic number. Consider defining it to a well named constant.");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("detekt-project:A.kt");
    assertThat(third.ruleKey().rule()).isEqualTo("EqualsWithHashCodeExist");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(third.primaryLocation().message()).isEqualTo("A class should always override hashCode when overriding equals and the other way around.");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(3);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  public void no_issues_without_report_paths_property() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, null);
    assertThat(externalIssues).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_with_invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("invalid-path.txt' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_checkstyle_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "not-checkstyle-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("not-checkstyle-file.xml' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "invalid-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("invalid-file.xml' can't be read.");
  }

  @Test
  public void issues_when_xml_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "detekt-checkstyle-with-errors.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("detekt-project:main.kt");
    assertThat(first.ruleKey().rule()).isEqualTo("UnknownRuleKey");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Error at file level with an unknown rule key.");
    assertThat(first.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder(
      "No input file found for not-existing-file.kt. No detekt issues will be imported on this file.");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Unexpected error without message for rule: 'detekt.EmptyIfBlock'",
      "Unexpected rule key without 'detekt.' suffix: 'invalid-format'");
  }

  private List<ExternalIssue> executeSensorImporting(int majorVersion, int minorVersion, @Nullable String fileName) throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR, majorVersion, minorVersion);
    if (fileName != null) {
      String path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString();
      context.settings().setProperty("sonar.kotlin.detekt.reportPaths", path);
    }
    detektSensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

}
