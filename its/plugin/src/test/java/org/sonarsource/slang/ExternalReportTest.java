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
package org.sonarsource.slang;

import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
public class ExternalReportTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/externalreport/";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void detekt() {
    SonarScanner sonarScanner = getSonarScanner(BASE_DIRECTORY, "detekt");
    sonarScanner.setProperty("sonar.kotlin.detekt.reportPaths", "detekt-checkstyle.xml");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues();
    boolean externalIssuesSupported = ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(7, 2);
    if (externalIssuesSupported) {
      assertThat(issues).hasSize(1);
      Issue issue = issues.get(0);
      assertThat(issue.componentKey()).isEqualTo("project:main.kt");
      assertThat(issue.ruleKey()).isEqualTo("external_detekt:ForEachOnRange");
      assertThat(issue.line()).isEqualTo(2);
      assertThat(issue.message()).isEqualTo("Using the forEach method on ranges has a heavy performance cost. Prefer using simple for loops.");
      assertThat(issue.severity()).isEqualTo("CRITICAL");
      assertThat(issue.debt()).isEqualTo("5min");
    } else {
      assertThat(issues).isEmpty();
    }
  }

  @Test
  public void android_lint() {
    SonarScanner sonarScanner = getSonarScanner(BASE_DIRECTORY, "androidlint");
    sonarScanner.setProperty("sonar.androidLint.reportPaths", "lint-results.xml");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues();
    boolean externalIssuesSupported = ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(7, 2);
    if (externalIssuesSupported) {
      assertThat(issues).hasSize(2);
      Issue first = issues.stream().filter(issue -> "project:main.kt".equals(issue.componentKey())).findFirst().orElse(null);
      assertThat(first.ruleKey()).isEqualTo("external_android-lint:UnusedAttribute");
      assertThat(first.line()).isEqualTo(2);
      assertThat(first.message()).isEqualTo("Attribute `required` is only used in API level 5 and higher (current min is 1)");
      assertThat(first.severity()).isEqualTo("MINOR");
      assertThat(first.debt()).isEqualTo("5min");

      Issue second = issues.stream().filter(issue -> "project:build.gradle".equals(issue.componentKey())).findFirst().orElse(null);
      assertThat(second.ruleKey()).isEqualTo("external_android-lint:GradleDependency");
      assertThat(second.line()).isEqualTo(3);
      assertThat(second.message()).isEqualTo("A newer version of com.android.support:recyclerview-v7 than 26.0.0 is available: 27.1.1");
      assertThat(second.severity()).isEqualTo("MINOR");
      assertThat(second.debt()).isEqualTo("5min");
    } else {
      assertThat(issues).isEmpty();
    }
  }

  @Test
  public void rubocop() {
    SonarScanner sonarScanner = getSonarScanner(BASE_DIRECTORY, "rubocop");
    sonarScanner.setProperty("sonar.ruby.rubocop.reportPaths", "rubocop-report.json");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues();
    boolean externalIssuesSupported = ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(7, 2);
    if (externalIssuesSupported) {
      assertThat(issues).hasSize(1);
      Issue first = issues.get(0);
      assertThat(first.ruleKey()).isEqualTo("external_rubocop:Security/YAMLLoad");
      assertThat(first.line()).isEqualTo(2);
      assertThat(first.message()).isEqualTo("Security/YAMLLoad: Prefer using `YAML.safe_load` over `YAML.load`.");
      assertThat(first.severity()).isEqualTo("MAJOR");
      assertThat(first.debt()).isEqualTo("5min");
    } else {
      assertThat(issues).isEmpty();
    }
  }

  @Test
  public void scalastyle() throws IOException {
    SonarScanner sonarScanner = getSonarScanner(BASE_DIRECTORY, "scalastyle");
    Path projectDir = new File(BASE_DIRECTORY, "scalastyle").toPath();
    Path scalastyleReportPath = createTemporaryReportFromTemplate(projectDir.resolve("scalastyle-output.xml"),
      "{ABSOLUTE_HELLO_WORLD_PATH}", projectDir.resolve("HelloWorld.scala").toRealPath().toString());
    sonarScanner.setProperty("sonar.scala.scalastyle.reportPaths", scalastyleReportPath.toString());
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues();
    boolean externalIssuesSupported = ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(7, 2);
    if (externalIssuesSupported) {
      assertThat(issues).hasSize(2);
      assertThat(issues.stream().map(Issue::ruleKey).sorted().collect(Collectors.toList())).containsExactly(
        "external_scalastyle:org.scalastyle.file.HeaderMatchesChecker",
        "external_scalastyle:org.scalastyle.file.RegexChecker"
      );
      assertThat(issues.stream().map(Issue::line).sorted().collect(Collectors.toList())).containsExactly(
        1,
        6
      );
      Issue first = issues.get(0);
      assertThat(first.debt()).isEqualTo("5min");
    } else {
      assertThat(issues).isEmpty();
    }
  }

  @Test
  public void scapegoat() throws IOException {
    SonarScanner sonarScanner = getSonarScanner(BASE_DIRECTORY, "scapegoat");
    Path projectDir = new File(BASE_DIRECTORY, "scapegoat").toPath();
    Path scapegoatReportPath = createTemporaryReportFromTemplate(projectDir.resolve("scapegoat-scalastyle.xml"),
      "{ABSOLUTE_HELLO_WORLD_PATH}", projectDir.resolve("HelloWorld.scala").toRealPath().toString());
    sonarScanner.setProperty("sonar.scala.scapegoat.reportPaths", scapegoatReportPath.toString());
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues();
    boolean externalIssuesSupported = ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(7, 2);
    if (externalIssuesSupported) {
      assertThat(issues).hasSize(3);
      assertThat(issues.stream().map(Issue::ruleKey).sorted().collect(Collectors.toList())).containsExactly(
        "external_scapegoat:com.sksamuel.scapegoat.inspections.EmptyCaseClass",
        "external_scapegoat:com.sksamuel.scapegoat.inspections.FinalModifierOnCaseClass",
        "external_scapegoat:com.sksamuel.scapegoat.inspections.unsafe.IsInstanceOf"
      );
      assertThat(issues.stream().map(Issue::line).sorted().collect(Collectors.toList())).containsExactly(
        5,
        9,
        9
      );
      Issue first = issues.get(0);
      assertThat(first.debt()).isEqualTo("5min");
    } else {
      assertThat(issues).isEmpty();
    }
  }

  private List<Issue> getExternalIssues() {
    Server server = ORCHESTRATOR.getServer();
    IssueClient issueClient = SonarClient.create(server.getUrl()).issueClient();
    return issueClient.find(IssueQuery.create().componentRoots(PROJECT_KEY)).list().stream()
      .filter(issue -> issue.ruleKey().startsWith("external_"))
      .collect(Collectors.toList());
  }

  private Path createTemporaryReportFromTemplate(Path sourceReportPath, String placeHolder, String newValue) throws IOException {
    String reportContent = new String(Files.readAllBytes(sourceReportPath), UTF_8);
    reportContent = reportContent.replace(placeHolder, newValue);
    Path destReportPath = tmpDir.newFile(sourceReportPath.getFileName().toString()).toPath().toRealPath();
    Files.write(destReportPath, reportContent.getBytes(UTF_8));
    return destReportPath;
  }

}
