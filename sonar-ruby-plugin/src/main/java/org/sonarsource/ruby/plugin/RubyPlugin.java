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
package org.sonarsource.ruby.plugin;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.Version;
import org.sonarsource.ruby.externalreport.rubocop.RuboCopRulesDefinition;
import org.sonarsource.ruby.externalreport.rubocop.RuboCopSensor;

public class RubyPlugin implements Plugin {

  public static final String RUBY_LANGUAGE_KEY = "ruby";
  static final String RUBY_LANGUAGE_NAME = "Ruby";

  static final String RUBY_FILE_SUFFIXES_DEFAULT_VALUE = ".rb";
  static final String RUBY_FILE_SUFFIXES_KEY = "sonar.ruby.file.suffixes";

  static final String REPORT_PATHS_DEFAULT_VALUE = "coverage/.resultset.json";
  static final String REPORT_PATHS_KEY = "sonar.ruby.coverage.reportPaths";

  static final String RUBY_REPOSITORY_KEY = "ruby";
  static final String REPOSITORY_NAME = "SonarAnalyzer";
  static final String PROFILE_NAME = "Sonar way";

  private static final String GENERAL = "General";
  private static final String RUBY_CATEGORY = "Ruby";
  private static final String TEST_COVERAGE_SUBCATEGORY = "Test and Coverage";
  private static final String EXTERNAL_ANALYZERS_CATEGORY = "External Analyzers";

  @Override
  public void define(Context context) {
    boolean externalIssuesSupported = context.getSonarQubeVersion().isGreaterThanOrEqual(Version.create(7, 2));

    context.addExtensions(
      RubyLanguage.class,
      RubySensor.class,
      RubyProfileDefinition.class,
      RubyRulesDefinition.class,
      new RuboCopRulesDefinition(externalIssuesSupported),
      RuboCopSensor.class,

      PropertyDefinition.builder(RUBY_FILE_SUFFIXES_KEY)
        .defaultValue(RUBY_FILE_SUFFIXES_DEFAULT_VALUE)
        .name("File Suffixes")
        .description("List of suffixes for files to analyze.")
        .subCategory(GENERAL)
        .category(RUBY_CATEGORY)
        .multiValues(true)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      PropertyDefinition.builder(REPORT_PATHS_KEY)
        .defaultValue(REPORT_PATHS_DEFAULT_VALUE)
        .name("Path to coverage report(s)")
        .description("Path to coverage report files (.resultset.json) generated by SimpleCov. The path may be absolute or relative to the project base directory.")
        .category(RUBY_CATEGORY)
        .subCategory(TEST_COVERAGE_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build()
    );

    if (externalIssuesSupported) {
      context.addExtension(
        PropertyDefinition.builder(RuboCopSensor.REPORT_PROPERTY_KEY)
          .name("RuboCop Report Files")
          .description("Paths (absolute or relative) to json files with RuboCop issues.")
          .category(EXTERNAL_ANALYZERS_CATEGORY)
          .subCategory(RUBY_CATEGORY)
          .onQualifiers(Qualifiers.PROJECT)
          .multiValues(true)
          .build());
    }
  }
}
