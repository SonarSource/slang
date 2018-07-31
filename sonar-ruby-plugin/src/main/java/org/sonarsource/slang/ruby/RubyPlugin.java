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
package org.sonarsource.slang.ruby;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class RubyPlugin implements Plugin {

  static final String RUBY_LANGUAGE_KEY = "ruby";
  static final String RUBY_LANGUAGE_NAME = "Ruby";

  static final String RUBY_FILE_SUFFIXES_DEFAULT_VALUE = ".rb";
  static final String RUBY_FILE_SUFFIXES_KEY = "sonar.ruby.file.suffixes";

  static final String RUBY_REPOSITORY_KEY = "ruby";
  static final String REPOSITORY_NAME = "SonarAnalyzer";
  static final String PROFILE_NAME = "Sonar way";

  private static final String GENERAL = "General";
  private static final String RUBY_CATEGORY = "Ruby";

  @Override
  public void define(Context context) {
    context.addExtensions(
      RubyLanguage.class,
      RubyProfileDefinition.class,
      RubyRulesDefinition.class,

      PropertyDefinition.builder(RUBY_FILE_SUFFIXES_KEY)
        .defaultValue(RUBY_FILE_SUFFIXES_DEFAULT_VALUE)
        .name("File Suffixes")
        .description("List of suffixes for files to analyze.")
        .subCategory(GENERAL)
        .category(RUBY_CATEGORY)
        .multiValues(true)
        .onQualifiers(Qualifiers.PROJECT)
        .build()
    );
  }
}
