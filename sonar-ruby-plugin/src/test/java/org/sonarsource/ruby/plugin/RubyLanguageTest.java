/*
 * SonarSource SLang
 * Copyright (C) 2018-2020 SonarSource SA
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
package org.sonarsource.ruby.plugin;

import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RubyLanguageTest {

  @Test
  public void test_suffixes_default() {
    RubyLanguage rubyLanguage = new RubyLanguage(new MapSettings().asConfig());
    assertThat(rubyLanguage.getFileSuffixes()).containsExactly(".rb");
  }

  @Test
  public void test_suffixes_empty() {
    RubyLanguage rubyLanguage = new RubyLanguage(new MapSettings().setProperty(RubyPlugin.RUBY_FILE_SUFFIXES_KEY, "").asConfig());
    assertThat(rubyLanguage.getFileSuffixes()).containsExactly(".rb");
  }

}
