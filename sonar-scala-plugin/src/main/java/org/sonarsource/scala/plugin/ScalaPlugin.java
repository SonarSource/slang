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

import org.sonar.api.Plugin;
import org.sonar.api.SonarProduct;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonarsource.scala.externalreport.scalastyle.ScalastyleRulesDefinition;
import org.sonarsource.scala.externalreport.scalastyle.ScalastyleSensor;
import org.sonarsource.scala.externalreport.scapegoat.ScapegoatRulesDefinition;
import org.sonarsource.scala.externalreport.scapegoat.ScapegoatSensor;

public class ScalaPlugin implements Plugin {
  public static final String SCALA_LANGUAGE_KEY = "scala";
  static final String SCALA_LANGUAGE_NAME = "Scala";

  static final String SCALA_FILE_SUFFIXES_DEFAULT_VALUE = ".scala";
  static final String SCALA_FILE_SUFFIXES_KEY = "sonar.scala.file.suffixes";

  static final String COVERAGE_REPORT_PATHS_KEY = "sonar.scala.coverage.reportPaths";

  static final String SCALA_REPOSITORY_KEY = "scala";
  static final String REPOSITORY_NAME = "SonarAnalyzer";
  static final String PROFILE_NAME = "Sonar way";

  private static final String GENERAL = "General";
  private static final String SCALA_CATEGORY = "Scala";
  private static final String TEST_COVERAGE_SUBCATEGORY = "Test and Coverage";
  private static final String EXTERNAL_ANALYZERS_CATEGORY = "External Analyzers";

  @Override
  public void define(Context context) {

    context.addExtensions(
      ScalaLanguage.class,
      ScalaSensor.class,
      ScalaRulesDefinition.class);

    if (context.getRuntime().getProduct() != SonarProduct.SONARLINT) {

      context.addExtensions(
        ScalaProfileDefinition.class,
        ScoverageSensor.class,
        ScalastyleSensor.class,
        ScapegoatSensor.class,
        ScalastyleRulesDefinition.class,
        ScapegoatRulesDefinition.class,

        PropertyDefinition.builder(SCALA_FILE_SUFFIXES_KEY)
          .defaultValue(SCALA_FILE_SUFFIXES_DEFAULT_VALUE)
          .name("File Suffixes")
          .description("List of suffixes for files to analyze.")
          .subCategory(GENERAL)
          .category(SCALA_CATEGORY)
          .multiValues(true)
          .onQualifiers(Qualifiers.PROJECT)
          .build(),

        PropertyDefinition.builder(COVERAGE_REPORT_PATHS_KEY)
          .name("Path to Scoverage report")
          .description("Path to Scoverage report file(s) (scoverage.xml). Usually in target\\scala-X.X\\scoverage-report")
          .category(SCALA_CATEGORY)
          .subCategory(TEST_COVERAGE_SUBCATEGORY)
          .onQualifiers(Qualifiers.PROJECT)
          .multiValues(true)
          .build(),

          PropertyDefinition.builder(ScalastyleSensor.REPORT_PROPERTY_KEY)
            .name("Scalastyle Report Files")
            .description("Paths (absolute or relative) to scalastyle xml files with Scalastyle issues.")
            .category(EXTERNAL_ANALYZERS_CATEGORY)
            .subCategory(SCALA_CATEGORY)
            .onQualifiers(Qualifiers.PROJECT)
            .multiValues(true)
            .build(),

          PropertyDefinition.builder(ScapegoatSensor.REPORT_PROPERTY_KEY)
            .name("Scapegoat Report Files")
            .description("Paths (absolute or relative) to scapegoat xml files using scalastyle format. For example: scapegoat-scalastyle.xml")
            .category(EXTERNAL_ANALYZERS_CATEGORY)
            .subCategory(SCALA_CATEGORY)
            .onQualifiers(Qualifiers.PROJECT)
            .multiValues(true)
            .build()
          );
      }
  }
}
