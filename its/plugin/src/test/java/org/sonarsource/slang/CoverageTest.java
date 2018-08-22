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
package org.sonarsource.slang;

import com.google.common.io.Files;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CoverageTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/measures/";
  private static final String ABSOLUTE_PATH_PLACEHOLDER = "{ABSOLUTE_PATH_PLACEHOLDER}";

  @Before
  public void setUp() throws Exception {
    File file = new File(BASE_DIRECTORY + "ruby/file.rb");
    File report = new File(BASE_DIRECTORY + "ruby/resultset.json");
    String reportContent = Files.toString(report, StandardCharsets.UTF_8);
    reportContent = reportContent.replace(ABSOLUTE_PATH_PLACEHOLDER, file.getAbsolutePath());
    Files.write(reportContent, report, StandardCharsets.UTF_8);
  }

  @Test
  public void ruby_coverage() {
    SonarScanner rubyScanner = getSonarScanner(BASE_DIRECTORY, "ruby");
    rubyScanner.setProperty("sonar.ruby.coverage.reportPaths", "resultset.json");
    ORCHESTRATOR.executeBuild(rubyScanner);

    assertThat(getMeasureAsInt("file.rb", "lines_to_cover")).isEqualTo(7);
    assertThat(getMeasureAsInt("file.rb", "uncovered_lines")).isEqualTo(1);
    assertThat(getMeasureAsInt("file.rb", "conditions_to_cover")).isNull();
    assertThat(getMeasureAsInt("file.rb", "uncovered_conditions")).isNull();

    assertThat(getMeasureAsInt("file_not_in_report.rb", "lines_to_cover")).isEqualTo(3);
    assertThat(getMeasureAsInt("file_not_in_report.rb", "uncovered_lines")).isEqualTo(3);
    assertThat(getMeasureAsInt("file_not_in_report.rb", "conditions_to_cover")).isNull();
    assertThat(getMeasureAsInt("file_not_in_report.rb", "uncovered_conditions")).isNull();
  }

  @After
  public void tearDown() throws Exception {
    File file = new File(BASE_DIRECTORY + "ruby/file.rb");
    File report = new File(BASE_DIRECTORY + "ruby/resultset.json");
    String reportContent = Files.toString(report, StandardCharsets.UTF_8);
    reportContent = reportContent.replace(file.getAbsolutePath(), ABSOLUTE_PATH_PLACEHOLDER);
    Files.write(reportContent, report, StandardCharsets.UTF_8);
  }

}
