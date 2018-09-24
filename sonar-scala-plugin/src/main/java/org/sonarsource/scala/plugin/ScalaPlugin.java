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
package org.sonarsource.scala.plugin;

import org.sonar.api.Plugin;
import org.sonar.api.SonarProduct;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class ScalaPlugin implements Plugin {
  public static final String SCALA_LANGUAGE_KEY = "scala";
  static final String SCALA_LANGUAGE_NAME = "Scala";

  static final String SCALA_FILE_SUFFIXES_DEFAULT_VALUE = ".scala";
  static final String SCALA_FILE_SUFFIXES_KEY = "sonar.scala.file.suffixes";

  static final String SCALA_REPOSITORY_KEY = "scala";
  static final String REPOSITORY_NAME = "SonarAnalyzer";
  static final String PROFILE_NAME = "Sonar way";

  private static final String GENERAL = "General";
  private static final String SCALA_CATEGORY = "Scala";

  @Override
  public void define(Context context) {

    context.addExtensions(
      ScalaLanguage.class,
      ScalaSensor.class,
      ScalaProfileDefinition.class,
      ScalaRulesDefinition.class);

    if (context.getRuntime().getProduct() != SonarProduct.SONARLINT) {
      context.addExtension(
        PropertyDefinition.builder(SCALA_FILE_SUFFIXES_KEY)
          .defaultValue(SCALA_FILE_SUFFIXES_DEFAULT_VALUE)
          .name("File Suffixes")
          .description("List of suffixes for files to analyze.")
          .subCategory(GENERAL)
          .category(SCALA_CATEGORY)
          .multiValues(true)
          .onQualifiers(Qualifiers.PROJECT)
          .build()
      );
    }

  }

}
