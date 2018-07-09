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

import com.sonar.orchestrator.build.SonarScanner;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSuffixesTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/measures/";
  private static final String FILES_METRIC = "files";

  @Test
  public void kotlin_file_suffixes_kt() {
    SonarScanner build = getSonarScanner(BASE_DIRECTORY, "kotlin", "norule-profile")
      .setProperty("sonar.kotlin.file.suffixes", ".kt");
    ORCHESTRATOR.executeBuild(build);

    assertThat(getMeasureAsInt(FILES_METRIC)).isEqualTo(3);
  }

  @Test
  public void kotlin_empty_file_suffixes() {
    SonarScanner build = getSonarScanner(BASE_DIRECTORY, "kotlin", "norule-profile")
      .setProperty("sonar.kotlin.file.suffixes", "");
    ORCHESTRATOR.executeBuild(build);

    assertThat(getMeasureAsInt(FILES_METRIC)).isEqualTo(3);
  }

  @Test
  public void kotlin_file_suffixes_ktt() {
    SonarScanner build = getSonarScanner(BASE_DIRECTORY, "kotlin", "norule-profile")
      .setProperty("sonar.kotlin.file.suffixes", ".ktt");
    ORCHESTRATOR.executeBuild(build);

    assertThat(getMeasureAsInt(FILES_METRIC)).isEqualTo(1);
  }

}
