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
package org.sonarsource.ruby.externalreport.rubocop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonarsource.slang.testing.ThreadLocalLogTester;
import org.sonarsource.ruby.plugin.RubyPlugin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class RuboCopSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "rubocop");

  private RuboCopSensor ruboCopSensor;
  private final List<String> analysisWarnings = new ArrayList<>();

  @BeforeEach
  void setup() {
    analysisWarnings.clear();
    ruboCopSensor = new RuboCopSensor(analysisWarnings::add);
  }

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    ruboCopSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of RuboCop issues");
    assertThat(sensorDescriptor.languages()).containsOnly("ruby");
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void issues_with_sonarqube() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("rubocop-report.json");
    assertThat(externalIssues).hasSize(4);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:useless-assignment.rb");
    assertThat(first.ruleKey()).hasToString("external_rubocop:Lint/UselessAssignment");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Lint/UselessAssignment: Useless assignment to variable - `param`.");
    assertThat(location(first)).isEqualTo("from line 3 offset 2 to line 3 offset 7");

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:useless-assignment.rb");
    assertThat(first.ruleKey()).hasToString("external_rubocop:Lint/UselessAssignment");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Lint/UselessAssignment: Useless assignment to variable - `param`.");
    assertThat(location(second)).isEqualTo("from line 130 offset 2 to line 130 offset 7");

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:yaml-issue.rb");
    assertThat(third.ruleKey()).hasToString("external_rubocop:Security/YAMLLoad");
    assertThat(third.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(third.primaryLocation().message()).isEqualTo("Security/YAMLLoad: Prefer using `YAML.safe_load` over `YAML.load`.");
    assertThat(location(third)).isEqualTo("from line 2 offset 7 to line 2 offset 11");

    ExternalIssue fourth = externalIssues.get(3);
    assertThat(fourth.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:yaml-issue.rb");
    assertThat(fourth.ruleKey()).hasToString("external_rubocop:Style/StringLiterals");
    assertThat(fourth.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(fourth.severity()).isEqualTo(Severity.MINOR);
    assertThat(fourth.primaryLocation().message()).isEqualTo("Style/StringLiterals: Prefer single-quoted strings when you don't need string interpolation or special symbols.");
    assertThat(location(fourth)).isEqualTo("from line 2 offset 12 to line 2 offset 21");

    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void no_issues_without_report_paths_property() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(null);
    assertThat(externalIssues).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void no_issues_with_invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(Level.WARN)))
      .startsWith("Unable to import RuboCop report file(s):")
      .contains("invalid-path.txt")
      .endsWith("The report file(s) can not be found. Check that the property 'sonar.ruby.rubocop.reportPaths' is correctly configured.");
    assertThat(analysisWarnings).hasSize(1);
    assertThat(analysisWarnings.get(0))
      .startsWith("Unable to import 1 RuboCop report file(s).")
      .endsWith("Please check that property 'sonar.ruby.rubocop.reportPaths' is correctly configured and the analysis logs for more details.");
  }

  @Test
  void no_issues_with_invalid_rubocop_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("not-rubocop-file.json");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(Level.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .contains("not-rubocop-file.json' can't be read.");
  }

  @Test
  void no_issues_with_empty_rubocop_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("rubocop-report-empty.json");
    assertThat(externalIssues).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void issues_when_rubocop_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("rubocop-report-with-errors.json");
    assertThat(externalIssues).hasSize(8);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("rubocop-project:useless-assignment.rb");
    assertThat(first.ruleKey()).hasToString("external_rubocop:ruleKey1");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("message 1");
    assertThat(location(first)).isEqualTo("from line 3 offset 2 to line 3 offset 7");

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(Level.WARN)))
      .isEqualTo("Fail to resolve 26 file(s). No RuboCop issues will be imported on the following file(s): " +
        "invalid-path-a.json;invalid-path-b.json;invalid-path-c.json;invalid-path-d.json;invalid-path-e.json;" +
        "invalid-path-f.json;invalid-path-g.json;invalid-path-h.json;invalid-path-i.json;invalid-path-j.json;" +
        "invalid-path-k.json;invalid-path-l.json;invalid-path-m.json;invalid-path-n.json;invalid-path-o.json;" +
        "invalid-path-p.json;invalid-path-q.json;invalid-path-r.json;invalid-path-s.json;invalid-path-t.json;...");
    assertThat(logTester.logs(Level.DEBUG)).containsExactlyInAnyOrder(
      "Missing information or unsupported file type for ruleKey:'NotEmptyRuleKey', filePath:'useless-assignment.rb', message:'null'",
      "Missing information or unsupported file type for ruleKey:'', filePath:'useless-assignment.rb', message:'Valid message.'",
      "Missing information or unsupported file type for ruleKey:'NotEmptyRuleKey', filePath:'null', message:'Valid message.'");
  }

  @Test
  void issues_when_rubocop_file_and_line_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("rubocop-report-with-file-and-line-errors.json");
    assertThat(externalIssues).hasSize(4);

    assertThat(location(externalIssues.get(0))).isEqualTo("from line 3 offset 2 to line 3 offset 7");
    assertThat(location(externalIssues.get(1))).isEqualTo("from line 3 offset 6 to line 4 offset 3");
    assertThat(location(externalIssues.get(2))).isEqualTo("from line 3 offset 0 to line 3 offset 15");
    assertThat(location(externalIssues.get(3))).isEqualTo("from line 3 offset 0 to line 3 offset 15");

    assertThat(onlyOneLogElement(logTester.logs(Level.ERROR))).contains("1000 is not a valid line for pointer. File useless-assignment.rb has 132 line(s)");
    assertThat(onlyOneLogElement(logTester.logs(Level.WARN)))
      .isEqualTo("Fail to resolve 1 file(s). No RuboCop issues will be imported on the following file(s): invalid-path.json");
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  private List<ExternalIssue> executeSensorImporting(@Nullable String fileName) throws IOException {
    SensorContextTester context = SensorContextTester.create(PROJECT_DIR);
    Files.list(PROJECT_DIR)
      .filter(file -> !Files.isDirectory(file))
      .forEach(file -> addFileToContext(context, PROJECT_DIR, file));
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
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(logTester.logs(Level.WARN)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
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
