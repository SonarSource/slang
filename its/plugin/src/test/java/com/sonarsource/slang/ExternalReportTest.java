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
package com.sonarsource.slang;

import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Server;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;
public class ExternalReportTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/externalreport/";

  @Test
  public void detekt() {
    SonarScanner sonarScanner = getSonarScanner(BASE_DIRECTORY, "detekt");
    sonarScanner.setProperty("sonar.kotlin.detekt.reportPaths", "detekt-checkstyle.xml");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues();
    if (ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(7, 2)) {
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
    sonarScanner.setProperty("sonar.android.androidLint.reportPaths", "lint-results.xml");
    ORCHESTRATOR.executeBuild(sonarScanner);
    List<Issue> issues = getExternalIssues();
    if (ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(7, 2)) {
      assertThat(issues).hasSize(1);
      Issue issue = issues.get(0);
      assertThat(issue.componentKey()).isEqualTo("project:main.kt");
      assertThat(issue.ruleKey()).isEqualTo("external_android-lint-kotlin:UnusedAttribute");
      assertThat(issue.line()).isEqualTo(2);
      assertThat(issue.message()).isEqualTo("Attribute `required` is only used in API level 5 and higher (current min is 1)");
      assertThat(issue.severity()).isEqualTo("MINOR");
      assertThat(issue.debt()).isEqualTo("5min");
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

}
