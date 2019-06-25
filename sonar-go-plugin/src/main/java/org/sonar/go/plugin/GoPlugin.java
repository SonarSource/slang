/*
 * SonarQube Go Plugin
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
package org.sonar.go.plugin;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.Version;

public class GoPlugin implements Plugin {

  static final String RESOURCE_FOLDER = "org/sonar/l10n/go/rules/go";

  public static final String EXCLUSIONS_KEY = "sonar.go.exclusions";
  public static final String EXCLUSIONS_DEFAULT_VALUE = "**/vendor/**";

  private static final String GO_CATEGORY = "Go";
  private static final String GENERAL_SUBCATEGORY = "General";

  @Override
  public void define(Context context) {
    boolean externalIssuesSupported = context.getSonarQubeVersion().isGreaterThanOrEqual(Version.create(7, 2));

    context.addExtensions(
      GoLanguage.class,
      GoSensor.class,
      GoExclusionsFileFilter.class,
      new GoRulesDefinition(externalIssuesSupported),
      GoProfileDefinition.class,

      PropertyDefinition.builder(GoLanguage.FILE_SUFFIXES_KEY)
        .index(10)
        .name("File Suffixes")
        .description("List of suffixes for files to analyze.")
        .category(GO_CATEGORY)
        .subCategory(GENERAL_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue(GoLanguage.FILE_SUFFIXES_DEFAULT_VALUE)
        .multiValues(true)
        .build(),

      PropertyDefinition.builder(EXCLUSIONS_KEY)
        .index(11)
        .defaultValue(EXCLUSIONS_DEFAULT_VALUE)
        .name("Go Exclusions")
        .description("List of file path patterns to be excluded from analysis of Go files.")
        .category(GO_CATEGORY)
        .subCategory(GENERAL_SUBCATEGORY)
        .onQualifiers(Qualifiers.MODULE, Qualifiers.PROJECT)
        .multiValues(true)
        .build());

  }
}
