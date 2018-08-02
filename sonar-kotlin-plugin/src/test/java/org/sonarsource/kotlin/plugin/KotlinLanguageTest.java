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
package org.sonarsource.kotlin.plugin;

import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class KotlinLanguageTest {

  @Test
  public void test_suffixes_default() throws Exception {
    KotlinLanguage kotlinLanguage = new KotlinLanguage(new MapSettings().asConfig());
    assertThat(kotlinLanguage.getFileSuffixes()).containsExactly(".kt");
  }

  @Test
  public void test_suffixes_empty() throws Exception {
    KotlinLanguage kotlinLanguage = new KotlinLanguage(new MapSettings().setProperty(KotlinPlugin.KOTLIN_FILE_SUFFIXES_KEY, "").asConfig());
    assertThat(kotlinLanguage.getFileSuffixes()).containsExactly(".kt");
  }

  @Test
  public void test_suffixes_custom() throws Exception {
    KotlinLanguage kotlinLanguage = new KotlinLanguage(new MapSettings().setProperty(KotlinPlugin.KOTLIN_FILE_SUFFIXES_KEY, ".foo, .bar").asConfig());
    assertThat(kotlinLanguage.getFileSuffixes()).containsExactly(".foo", ".bar");
  }

  @Test
  public void test_key_and_name() throws Exception {
    KotlinLanguage kotlinLanguage = new KotlinLanguage(new MapSettings().asConfig());
    assertThat(kotlinLanguage.getKey()).isEqualTo("kotlin");
    assertThat(kotlinLanguage.getName()).isEqualTo("Kotlin");
  }
}
