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
package org.sonarsource.ruby.externalreport.rubocop;

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
import org.sonar.api.batch.fs.TextRange;
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
import org.sonarsource.ruby.plugin.RubyPlugin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class RuboCopSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "rubocop");

  private static RuboCopSensor ruboCopSensor = new RuboCopSensor();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    ruboCopSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of RuboCop issues");
    assertThat(sensorDescriptor.languages()).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_with_sonarqube_71() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 1, "rubocop-report.json");
    assertThat(externalIssues).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactly("Import of external issues requires SonarQube 7.2 or greater.");
  }

  @Test
  public void issues_with_sonarqube_72() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "rubocop-report.json");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:useless-assignment.rb");
    assertThat(first.ruleKey().toString()).isEqualTo("rubocop:Lint/UselessAssignment");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Lint/UselessAssignment: Useless assignment to variable - `param`.");
    TextRange firstRange = first.primaryLocation().textRange();
    assertThat(firstRange.start().line()).isEqualTo(3);
    assertThat(firstRange.start().lineOffset()).isEqualTo(2);
    assertThat(firstRange.end().line()).isEqualTo(3);
    assertThat(firstRange.end().lineOffset()).isEqualTo(7);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:yaml-issue.rb");
    assertThat(second.ruleKey().toString()).isEqualTo("rubocop:Security/YAMLLoad");
    assertThat(second.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.primaryLocation().message()).isEqualTo("Security/YAMLLoad: Prefer using `YAML.safe_load` over `YAML.load`.");
    TextRange secondRange = second.primaryLocation().textRange();
    assertThat(secondRange.start().line()).isEqualTo(2);
    assertThat(secondRange.start().lineOffset()).isEqualTo(7);
    assertThat(secondRange.end().line()).isEqualTo(2);
    assertThat(secondRange.end().lineOffset()).isEqualTo(11);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:yaml-issue.rb");
    assertThat(third.ruleKey().toString()).isEqualTo("rubocop:Style/StringLiterals");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.MINOR);
    assertThat(third.primaryLocation().message()).isEqualTo("Style/StringLiterals: Prefer single-quoted strings when you don't need string interpolation or special symbols.");
    TextRange thirdRange = third.primaryLocation().textRange();
    assertThat(thirdRange.start().line()).isEqualTo(2);
    assertThat(thirdRange.start().lineOffset()).isEqualTo(12);
    assertThat(thirdRange.end().line()).isEqualTo(2);
    assertThat(thirdRange.end().lineOffset()).isEqualTo(21);

    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_without_report_paths_property() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7,2,null);
    assertThat(externalIssues).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void no_issues_with_invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7,2,"invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .contains("invalid-path.txt' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_rubocop_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7,2,"not-rubocop-file.json");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .contains("not-rubocop-file.json' can't be read.");
  }

  @Test
  public void no_issues_with_empty_rubocop_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7,2,"rubocop-report-empty.json");
    assertThat(externalIssues).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void issues_when_rubocop_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7,2,"rubocop-report-with-errors.json");
    assertThat(externalIssues).hasSize(7);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:useless-assignment.rb");
    assertThat(first.ruleKey().toString()).isEqualTo("rubocop:ruleKey1");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("message 1");
    TextRange firstRange = first.primaryLocation().textRange();
    assertThat(firstRange.start().line()).isEqualTo(3);
    assertThat(firstRange.start().lineOffset()).isEqualTo(2);
    assertThat(firstRange.end().line()).isEqualTo(3);
    assertThat(firstRange.end().lineOffset()).isEqualTo(7);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
      .isEqualTo("No input file found for invalid-path.json. No RuboCop issues will be imported on this file.");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Missing information or unsupported file type for ruleKey:'NotEmptyRuleKey', filePath:'useless-assignment.rb', message:'null'",
      "Missing information or unsupported file type for ruleKey:'', filePath:'useless-assignment.rb', message:'Valid message.'",
      "Missing information or unsupported file type for ruleKey:'NotEmptyRuleKey', filePath:'null', message:'Valid message.'");
  }

  @Test
  public void issues_when_rubocop_file_line_error() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7,2,"rubocop-report-with-line-error.json");
    assertThat(externalIssues).hasSize(0);

    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR))).contains("100 is not a valid line for pointer. File useless-assignment.rb has 5 line(s)");
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  private List<ExternalIssue> executeSensorImporting(int majorVersion, int minorVersion, @Nullable String fileName) throws IOException {
    SensorContextTester context = SensorContextTester.create(PROJECT_DIR);
    Files.list(PROJECT_DIR)
      .forEach(file -> addFileToContext(context, PROJECT_DIR, file));
    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(majorVersion, minorVersion), SonarQubeSide.SERVER));
    if (fileName != null) {
      String path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString();
      context.settings().setProperty("sonar.ruby.rubocop.reportPaths", path);
    }
    ruboCopSensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

  public static String onlyOneLogElement(List<String> elements) {
    assertThat(elements).hasSize(1);
    return elements.get(0);
  }

  public static void assertNoErrorWarnDebugLogs(LogTester logTester) {
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  private static void addFileToContext(SensorContextTester context, Path projectDir, Path file) {
    try {
      String projectId = projectDir.getFileName().toString() + "-project";
      context.fileSystem().add(TestInputFileBuilder.create(projectId, projectDir.toFile(), file.toFile())
        .setCharset(UTF_8)
        .setLanguage(language(file))
        .setContents(new String(Files.readAllBytes(file), UTF_8))
        .setType(InputFile.Type.MAIN)
        .build());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String language(Path file) {
    String path = file.toString();
    if (path.endsWith(".rb")) {
      return RubyPlugin.RUBY_LANGUAGE_KEY;
    }
    return path.substring(path.lastIndexOf('.') + 1);
  }

}
