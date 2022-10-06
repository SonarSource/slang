/*
 * SonarSource SLang
 * Copyright (C) 2018-2022 SonarSource SA
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
package org.sonarsource.scala.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

class ScalaLanguageTest {

  @Test
  void test_suffixes_default() {
    ScalaLanguage scalaLanguage = new ScalaLanguage(new MapSettings().asConfig());
    assertThat(scalaLanguage.getFileSuffixes()).containsExactly(".scala");
  }

  @Test
  void test_suffixes_empty() {
    ScalaLanguage scalaLanguage = new ScalaLanguage(new MapSettings().setProperty(ScalaPlugin.SCALA_FILE_SUFFIXES_KEY, "").asConfig());
    assertThat(scalaLanguage.getFileSuffixes()).containsExactly(".scala");
  }

  @Test
  void test_suffixes_custom() {
    ScalaLanguage scalaLanguage = new ScalaLanguage(new MapSettings().setProperty(ScalaPlugin.SCALA_FILE_SUFFIXES_KEY, ".custom").asConfig());
    assertThat(scalaLanguage.getFileSuffixes()).containsExactly(".custom");
  }

}
