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
package org.sonarsource.scala.externalreport.scalastyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ScalastyleSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "scalastyle");

  private static final String VIRTUAL_FILE_SYSTEM = "/absolute/path/to";

  private static ScalastyleFamilySensor sensor = new ScalastyleSensor();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test_config() {
    assertThat(sensor.reportLinterKey()).isEqualTo("scalastyle");
    assertThat(sensor.reportLinterName()).isEqualTo("Scalastyle");
    assertThat(sensor.reportPropertyKey()).isEqualTo("sonar.scala.scalastyle.reportPaths");
  }

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of Scalastyle issues");
    assertThat(sensorDescriptor.languages()).containsExactly("scala");
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_with_sonarqube_71() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 1, "scalastyle-output.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactly("Import of external issues requires SonarQube 7.2 or greater.");
  }

  @Test
  public void scalastyle_issues_with_sonarqube_72() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "scalastyle-output.xml");
    assertThat(externalIssues).hasSize(2);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("project:HelloWorld.scala");
    assertThat(first.ruleKey().toString()).isEqualTo("scalastyle:org.scalastyle.file.HeaderMatchesChecker");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Header does not match expected text");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(1);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("project:HelloWorld.scala");
    assertThat(second.ruleKey().toString()).isEqualTo("scalastyle:org.scalastyle.file.RegexChecker");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.MINOR);
    assertThat(second.primaryLocation().message()).isEqualTo("Regular expression matched 'println'");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(6);

    assertNoErrorWarnDebugLogs(logTester);
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
    String realPath = PROJECT_DIR.toRealPath().resolve("invalid-path.txt").toString();
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith(
      "No issues information will be saved as the report file '" + realPath + "' can't be read. FileNotFoundException: ");
  }

  @Test
  public void no_issues_with_invalid_scalastyle_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "invalid-scalastyle.txt");
    assertThat(externalIssues).isEmpty();
    String realPath = PROJECT_DIR.toRealPath().resolve("invalid-scalastyle.txt").toString();
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith(
      "No issues information will be saved as the report file '" + realPath + "' can't be read. XMLStreamException: ");
  }

  @Test
  public void no_issues_with_invalid_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "invalid.xml");
    assertThat(externalIssues).isEmpty();
    String realPath = PROJECT_DIR.toRealPath().resolve("invalid.xml").toString();
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactlyInAnyOrder(
      "No issues information will be saved as the report file '" + realPath + "' can't be read. " +
        "IOException: Unexpected document root 'invalid' instead of 'checkstyle'.");
  }

  @Test
  public void issues_when_xml_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "scalastyle-with-errors.xml");
    assertThat(externalIssues).hasSize(2);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("project:HelloWorld.scala");
    assertThat(first.ruleKey().toString()).isEqualTo("scalastyle:com.sksamuel.scapegoat.inspections.EmptyCaseClass");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Valid issue");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(5);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("project:HelloWorld.scala");
    assertThat(second.ruleKey().toString()).isEqualTo("scalastyle:unknown.key");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.primaryLocation().message()).isEqualTo("Missing line");
    assertThat(second.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder(
      "Fail to resolve 1 file path(s) in Scalastyle report. No issues imported related to file(s): /absolute/path/to/InvalidPath.scala");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Missing information or unsupported file type for source:'', file:'/absolute/path/to/HelloWorld.scala', message:'Missing source'",
      "Missing information or unsupported file type for source:'com.sksamuel.scapegoat.inspections.EmptyCaseClass', file:'/absolute/path/to/HelloWorld.scala', message:''",
      "Missing information or unsupported file type for source:'', file:'/absolute/path/to/HelloWorld.scala', message:''");
  }

  @Test
  public void invalid_line_number() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "scalastyle-invalid-line.xml");
    assertThat(externalIssues).isEmpty();
    String realPath = PROJECT_DIR.toRealPath().resolve("scalastyle-invalid-line.xml").toString();
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith(
      "No issues information will be saved as the report file '" + realPath + "' can't be read. NumberFormatException: ");
  }

  @Test
  public void issues_when_xml_file_has_a_lot_of_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "scalastyle-with-a-lot-of-errors.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder("" +
      "Fail to resolve 30 file path(s) in Scalastyle report. No issues imported related to file(s): " +
      "/absolute/path/to/InvalidPath00.scala;/absolute/path/to/InvalidPath01.scala;/absolute/path/to/InvalidPath02.scala;" +
      "/absolute/path/to/InvalidPath03.scala;/absolute/path/to/InvalidPath04.scala;/absolute/path/to/InvalidPath05.scala;" +
      "/absolute/path/to/InvalidPath06.scala;/absolute/path/to/InvalidPath07.scala;/absolute/path/to/InvalidPath08.scala;" +
      "/absolute/path/to/InvalidPath09.scala;/absolute/path/to/InvalidPath10.scala;/absolute/path/to/InvalidPath11.scala;" +
      "/absolute/path/to/InvalidPath12.scala;/absolute/path/to/InvalidPath13.scala;/absolute/path/to/InvalidPath14.scala;" +
      "/absolute/path/to/InvalidPath15.scala;/absolute/path/to/InvalidPath16.scala;/absolute/path/to/InvalidPath17.scala;" +
      "/absolute/path/to/InvalidPath18.scala;/absolute/path/to/InvalidPath19.scala;...");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  public List<ExternalIssue> executeSensorImporting(int majorVersion, int minorVersion, @Nullable String fileName) throws IOException {
    return executeSensorImporting(sensor, majorVersion, minorVersion, fileName);
  }

  public static List<ExternalIssue> executeSensorImporting(ScalastyleFamilySensor sensor, int majorVersion, int minorVersion, @Nullable String fileName) throws IOException {
    SensorContextTester context = SensorContextTester.create(PROJECT_DIR.toRealPath());
    DefaultFileSystem defaultFileSystem = new DefaultFileSystem(new File(VIRTUAL_FILE_SYSTEM));
    defaultFileSystem.add(inputFile("HelloWorld.scala"));
    context.setFileSystem(defaultFileSystem);
    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(majorVersion, minorVersion), SonarQubeSide.SERVER));
    if (fileName != null) {
      String path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString();
      context.settings().setProperty(sensor.reportPropertyKey(), path);
    }
    sensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

  private static DefaultInputFile inputFile(String fileName) throws IOException {
    String content = new String(Files.readAllBytes(PROJECT_DIR.resolve(fileName)), UTF_8);
    return TestInputFileBuilder.create("project", fileName)
      .setType(InputFile.Type.MAIN)
      .initMetadata(content)
      .setContents(content)
      .build();
  }

  public static void assertNoErrorWarnDebugLogs(LogTester logTester) {
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

}
