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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NoSonarTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/nosonar/";
  private static final String NO_SONAR_PROFILE_NAME = "nosonar-profile";
  private static final String RULE_KEY = "S2068";

  @Test
  public void test_kotlin_nosonar() {
    checkForLanguage("kotlin");
  }

  @Test
  public void test_go_nosonar() {
    checkForLanguage("go");
  }

  @Test
  public void test_ruby_nosonar() {
    checkForLanguage("ruby");
  }

  @Test
  public void test_scala_nosonar() {
    checkForLanguage("scala");
  }

  private void checkForLanguage(String language) {
    ORCHESTRATOR.executeBuild(getSonarScanner(BASE_DIRECTORY, language, NO_SONAR_PROFILE_NAME));

    assertThat(getMeasureAsInt("files")).isEqualTo(1);
    assertThat(getIssuesForRule(language + ":" + RULE_KEY)).hasSize(1);
  }
}
