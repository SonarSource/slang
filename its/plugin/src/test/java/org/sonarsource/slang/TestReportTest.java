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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestReportTest extends TestBase {

  private static final Path BASE_DIRECTORY = Paths.get("projects","measures");

  @ClassRule
  public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

  @Test
  public void go_test_report() {
    SonarScanner goScanner = getSonarScanner(BASE_DIRECTORY.toString(), "go");
    goScanner.setProperty("sonar.tests", ".");
    goScanner.setProperty("sonar.test.inclusions", "**/*_test.go");
    goScanner.setProperty("sonar.go.tests.reportPaths", "go-test-report.out");

    ORCHESTRATOR.executeBuild(goScanner);

    assertThat(getMeasureAsInt("tests")).isEqualTo(4);
    assertThat(getMeasureAsInt("test_failures")).isEqualTo(2);
    assertThat(getMeasureAsInt("test_errors")).isNull();
    assertThat(getMeasureAsInt("skipped_tests")).isEqualTo(1);
    assertThat(getMeasureAsInt("test_execution_time")).isEqualTo(4);

    assertThat(getMeasureAsInt("pivot_test.go", "tests")).isEqualTo(4);
    assertThat(getMeasureAsInt("pivot_test.go", "test_failures")).isEqualTo(2);
    assertThat(getMeasureAsInt("pivot_test.go", "test_errors")).isEqualTo(0);
    assertThat(getMeasureAsInt("pivot_test.go", "skipped_tests")).isEqualTo(1);
    assertThat(getMeasureAsInt("pivot_test.go", "test_execution_time")).isEqualTo(4);
  }

}
