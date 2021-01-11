/*
 * SonarQube Go Plugin
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

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.WildcardPattern;

public class RubyExclusionsFileFilter implements InputFileFilter {

  private final WildcardPattern[] excludedPatterns;

  public RubyExclusionsFileFilter(Configuration configuration) {
    excludedPatterns = WildcardPattern.create(configuration.getStringArray(RubyPlugin.EXCLUSIONS_KEY));
  }

  @Override
  public boolean accept(InputFile inputFile) {
    return isNotRubyFile(inputFile) || isNotExcluded(inputFile.uri().toString());
  }

  private static boolean isNotRubyFile(InputFile inputFile) {
    return !RubyPlugin.RUBY_LANGUAGE_KEY.equals(inputFile.language());
  }

  public boolean isNotExcluded(String filePath) {
    return !WildcardPattern.match(excludedPatterns, filePath);
  }
}
