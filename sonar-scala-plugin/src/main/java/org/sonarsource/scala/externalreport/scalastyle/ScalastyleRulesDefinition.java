/*
 * SonarSource SLang
 * Copyright (C) 2018-2023 SonarSource SA
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

import org.sonarsource.analyzer.commons.ExternalRuleLoader;
import org.sonarsource.scala.plugin.ScalaPlugin;

public class ScalastyleRulesDefinition extends ScalastyleFamilyRulesDefinition {

  private static final String RULES_JSON = "org/sonar/l10n/scala/rules/scalastyle/rules.json";

  static final ExternalRuleLoader RULE_LOADER = new ExternalRuleLoader(
    ScalastyleSensor.LINTER_KEY, ScalastyleSensor.LINTER_NAME, RULES_JSON, ScalaPlugin.SCALA_LANGUAGE_KEY);

  public ScalastyleRulesDefinition() {
    super(RULE_LOADER);
  }

}
