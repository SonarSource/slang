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

import java.util.List;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasuresTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/measures/";

  @Test
  public void kotlin_measures() {
    ORCHESTRATOR.executeBuild(getSonarScanner(BASE_DIRECTORY, "kotlin"));

    assertThat(getMeasureAsInt("files")).isEqualTo(3);

    assertThat(getMeasure("empty_file.kt", "ncloc")).isNull();
    assertThat(getMeasureAsInt("file1.kt", "ncloc")).isEqualTo(6);
    assertThat(getMeasureAsInt("file2.kt", "ncloc")).isEqualTo(8);

    assertThat(getMeasure("empty_file.kt", "comment_lines")).isNull();
    assertThat(getMeasureAsInt("file1.kt", "comment_lines")).isEqualTo(8);
    assertThat(getMeasureAsInt("file2.kt", "comment_lines")).isEqualTo(3);

    assertThat(getMeasure("empty_file.kt", "statements")).isNull();
    assertThat(getMeasureAsInt("file1.kt", "statements")).isEqualTo(3);
    assertThat(getMeasureAsInt("file2.kt", "statements")).isEqualTo(2);

    assertThat(getMeasureAsInt("file1.kt", "cognitive_complexity")).isEqualTo(0);
    assertThat(getMeasureAsInt("file2.kt", "cognitive_complexity")).isEqualTo(3);

    assertThat(getMeasure("empty_file.kt", "ncloc_data")).isNull();
    assertThat(getMeasure("file1.kt", "ncloc_data").getValue()).isEqualTo("2=1;3=1;6=1;7=1;12=1;13=1");
    assertThat(getMeasure("file2.kt", "ncloc_data").getValue()).isEqualTo("1=1;2=1;3=1;4=1;5=1;7=1;10=1;11=1");

    assertThat(getMeasure("file1.kt", "executable_lines_data").getValue()).isEqualTo("3=1;7=1;12=1");

    List<Issue> issuesForRule = getIssuesForRule("kotlin:S100");
    String file2Component = PROJECT_KEY + ":file2.kt";
    assertThat(issuesForRule).extracting(Issue::line).containsExactly(2, 7);
    assertThat(issuesForRule).extracting(Issue::componentKey).containsExactly(file2Component, file2Component);
  }

  @Test
  public void ruby_measures() {
    ORCHESTRATOR.executeBuild(getSonarScanner(BASE_DIRECTORY, "ruby"));

    assertThat(getMeasureAsInt("files")).isEqualTo(2);
    assertThat(getMeasureAsInt("file.rb", "ncloc")).isEqualTo(8);
    assertThat(getMeasureAsInt("file.rb", "comment_lines")).isEqualTo(12);
    assertThat(getMeasureAsInt("file.rb", "statements")).isEqualTo(5);
    assertThat(getMeasureAsInt("file.rb", "cognitive_complexity")).isEqualTo(0);
    assertThat(getMeasure("file.rb", "ncloc_data").getValue()).isEqualTo("16=1;2=1;3=1;20=1;6=1;7=1;14=1;15=1");
    assertThat(getMeasure("file.rb", "executable_lines_data").getValue()).isEqualTo("3=1;20=1;7=1;14=1;15=1");

    List<Issue> issuesForRule = getIssuesForRule("ruby:S1135");
    assertThat(issuesForRule).extracting(Issue::line).containsExactly(18);
    assertThat(issuesForRule).extracting(Issue::componentKey).containsExactly(PROJECT_KEY + ":file.rb");
  }

}
