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
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.ThreadLocalLogTester;
import org.sonarsource.ruby.plugin.RubyPlugin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class RuboCopSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "rubocop");

  private static RuboCopSensor ruboCopSensor = new RuboCopSensor();

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

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
    assertThat(location(first)).isEqualTo("from line 3 offset 2 to line 3 offset 7");

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:yaml-issue.rb");
    assertThat(second.ruleKey().toString()).isEqualTo("rubocop:Security/YAMLLoad");
    assertThat(second.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.primaryLocation().message()).isEqualTo("Security/YAMLLoad: Prefer using `YAML.safe_load` over `YAML.load`.");
    assertThat(location(second)).isEqualTo("from line 2 offset 7 to line 2 offset 11");

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:yaml-issue.rb");
    assertThat(third.ruleKey().toString()).isEqualTo("rubocop:Style/StringLiterals");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.MINOR);
    assertThat(third.primaryLocation().message()).isEqualTo("Style/StringLiterals: Prefer single-quoted strings when you don't need string interpolation or special symbols.");
    assertThat(location(third)).isEqualTo("from line 2 offset 12 to line 2 offset 21");

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
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .contains("invalid-path.txt' can't be read.");
  }

  @Test
  public void no_issues_with_invalid_rubocop_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "not-rubocop-file.json");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .contains("not-rubocop-file.json' can't be read.");
  }

  @Test
  public void no_issues_with_empty_rubocop_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "rubocop-report-empty.json");
    assertThat(externalIssues).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  public void issues_when_rubocop_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "rubocop-report-with-errors.json");
    assertThat(externalIssues).hasSize(7);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:useless-assignment.rb");
    assertThat(first.ruleKey().toString()).isEqualTo("rubocop:ruleKey1");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("message 1");
    assertThat(location(first)).isEqualTo("from line 3 offset 2 to line 3 offset 7");

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
      .isEqualTo("Fail to resolve 26 file(s). No RuboCop issues will be imported on the following file(s): " +
        "invalid-path-a.json;invalid-path-b.json;invalid-path-c.json;invalid-path-d.json;invalid-path-e.json;" +
        "invalid-path-f.json;invalid-path-g.json;invalid-path-h.json;invalid-path-i.json;invalid-path-j.json;" +
        "invalid-path-k.json;invalid-path-l.json;invalid-path-m.json;invalid-path-n.json;invalid-path-o.json;" +
        "invalid-path-p.json;invalid-path-q.json;invalid-path-r.json;invalid-path-s.json;invalid-path-t.json;...");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Missing information or unsupported file type for ruleKey:'NotEmptyRuleKey', filePath:'useless-assignment.rb', message:'null'",
      "Missing information or unsupported file type for ruleKey:'', filePath:'useless-assignment.rb', message:'Valid message.'",
      "Missing information or unsupported file type for ruleKey:'NotEmptyRuleKey', filePath:'null', message:'Valid message.'");
  }

  @Test
  public void issues_when_rubocop_file_and_line_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(7, 2, "rubocop-report-with-file-and-line-errors.json");
    assertThat(externalIssues).hasSize(4);

    assertThat(location(externalIssues.get(0))).isEqualTo("from line 3 offset 2 to line 3 offset 7");
    assertThat(location(externalIssues.get(1))).isEqualTo("from line 3 offset 6 to line 4 offset 3");
    assertThat(location(externalIssues.get(2))).isEqualTo("from line 3 offset 0 to line 3 offset 15");
    assertThat(location(externalIssues.get(3))).isEqualTo("from line 3 offset 0 to line 3 offset 15");

    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR))).contains("100 is not a valid line for pointer. File useless-assignment.rb has 5 line(s)");
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
      .isEqualTo("Fail to resolve 1 file(s). No RuboCop issues will be imported on the following file(s): invalid-path.json");
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

  public static void assertNoErrorWarnDebugLogs(ThreadLocalLogTester logTester) {
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

  private static String location(ExternalIssue issue) {
    TextRange range = issue.primaryLocation().textRange();
    if (range == null) {
      return "null";
    }
    return "from line " + range.start().line() + " offset " + range.start().lineOffset()
      + " to line " + range.end().line() + " offset " + range.end().lineOffset();
  }

}
