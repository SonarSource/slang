/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.scala.externalreport.scalastyle;

import org.sonar.api.notifications.AnalysisWarnings;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

public class ScalastyleSensor extends ScalastyleFamilySensor {

  static final String LINTER_KEY = "scalastyle";
  static final String LINTER_NAME = "Scalastyle";

  public static final String REPORT_PROPERTY_KEY = "sonar.scala.scalastyle.reportPaths";

  public ScalastyleSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, LINTER_KEY, LINTER_NAME, REPORT_PROPERTY_KEY);
  }

  @Override
  public ExternalRuleLoader ruleLoader() {
    return ScalastyleRulesDefinition.RULE_LOADER;
  }

}
