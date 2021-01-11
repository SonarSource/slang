/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class RubyExclusionsFileFilterTest {
  private final MapSettings settings = new MapSettings();

  @Before
  public void setup() {
    settings.clear();
  }

  @Test
  public void should_exclude_vendor_dir(){
    settings.setProperty(RubyPlugin.EXCLUSIONS_KEY, RubyPlugin.EXCLUSIONS_DEFAULT_VALUE);
    RubyExclusionsFileFilter filter = new RubyExclusionsFileFilter(settings.asConfig());
    assertThat(accept(filter, "file.rb")).isTrue();
    assertThat(accept(filter, "vendor/file.rb")).isFalse();
    assertThat(accept(filter, "vendor/someDir/file.rb")).isFalse();
    assertThat(accept(filter, "someDir/vendor/file.rb")).isFalse();
  }

  @Test
  public void should_exclude_only_ruby(){
    settings.setProperty(RubyPlugin.EXCLUSIONS_KEY, RubyPlugin.EXCLUSIONS_DEFAULT_VALUE);
    RubyExclusionsFileFilter filter = new RubyExclusionsFileFilter(settings.asConfig());
    assertThat(accept(filter, "vendor/file.rb")).isFalse();
    assertThat(accept(filter, "vendor/file.json")).isTrue();
  }

  @Test
  public void should_include_vendor_when_property_is_overridden(){
    settings.setProperty(RubyPlugin.EXCLUSIONS_KEY, "");
    RubyExclusionsFileFilter filter = new RubyExclusionsFileFilter(settings.asConfig());

    assertThat(accept(filter, "file.rb")).isTrue();
    assertThat(accept(filter, "vendor/file.rb")).isTrue();
    assertThat(accept(filter, "vendor/someDir/file.rb")).isTrue();
    assertThat(accept(filter, "someDir/vendor/file.rb")).isTrue();
  }

  @Test
  public void should_exclude_using_custom_path_regex(){
    settings.setProperty(RubyPlugin.EXCLUSIONS_KEY, "**/lib/**");
    RubyExclusionsFileFilter filter = new RubyExclusionsFileFilter(settings.asConfig());

    assertThat(accept(filter, "file.rb")).isTrue();
    assertThat(accept(filter, "vendor/file.rb")).isTrue();
    assertThat(accept(filter, "lib/file.rb")).isFalse();
    assertThat(accept(filter, "someDir/lib/file.rb")).isFalse();
  }

  @Test
  public void should_handle_multiple_path_regex(){
    settings.setProperty(RubyPlugin.EXCLUSIONS_KEY, "," + RubyPlugin.EXCLUSIONS_DEFAULT_VALUE + ",**/lib/**,");
    RubyExclusionsFileFilter filter = new RubyExclusionsFileFilter(settings.asConfig());

    assertThat(accept(filter, "file.rb")).isTrue();
    assertThat(accept(filter, "vendor/file.rb")).isFalse();
    assertThat(accept(filter, "lib/file.rb")).isFalse();
  }

  private static boolean accept(InputFileFilter filter, String file) {
    return filter.accept(inputFile(file));
  }

  private static InputFile inputFile(String file) {
    String extension = file.substring(file.lastIndexOf('.'));
    String language = RubyPlugin.RUBY_FILE_SUFFIXES_DEFAULT_VALUE.equals(extension) ? RubyPlugin.RUBY_LANGUAGE_KEY : "other";
    return new TestInputFileBuilder("test", "test_vendor/" + file).setLanguage(language).build();
  }
}
