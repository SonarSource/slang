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
package com.sonarsource.slang.kotlin;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class KotlinPlugin implements Plugin {

  // Subcategories
  private static final String GENERAL = "General";
  private static final String KOTLIN_CATEGORY = "Kotlin";

  // Global constants
  private static final String PLUGIN_KEY = "kotlin";
  public static final String LANGUAGE_KEY = "kotlin";
  public static final String LANGUAGE_NAME = "Kotlin";
  public static final String REPOSITORY_KEY = "kotlin";
  public static final String REPOSITORY_NAME = "SonarAnalyzer";
  public static final String PROFILE_NAME = "Sonar way";

  public static final String FILE_SUFFIXES_KEY = "sonar.kotlin.file.suffixes";
  public static final String FILE_SUFFIXES_DEFAULT_VALUE = ".kt";

  @Override
  public void define(Context context) {
    context.addExtensions(
      KotlinLanguage.class,
      KotlinSensor.class,
      KotlinRulesDefinition.class,
      KotlinProfileDefinition.class,
      PropertyDefinition.builder(FILE_SUFFIXES_KEY)
        .defaultValue(FILE_SUFFIXES_DEFAULT_VALUE)
        .name("File Suffixes")
        .description("Comma-separated list of suffixes for files to analyze.")
        .subCategory(GENERAL)
        .category(KOTLIN_CATEGORY)
        .multiValues(true)
        .onQualifiers(Qualifiers.PROJECT)
        .build());
  }

}
