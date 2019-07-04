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

import java.util.List;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.go.externalreport.AbstractReportSensor;
import org.sonar.go.externalreport.GoLintReportSensor;
import org.sonar.go.externalreport.GoVetReportSensor;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;
import org.sonarsource.slang.checks.utils.Language;
import org.sonarsource.slang.plugin.RulesDefinitionUtils;

public class GoRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "go";

  private boolean externalIssuesSupported;

  public GoRulesDefinition(boolean externalIssuesSupported) {
    this.externalIssuesSupported = externalIssuesSupported;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, GoLanguage.KEY)
      .setName("SonarAnalyzer");
    RuleMetadataLoader metadataLoader = new RuleMetadataLoader(GoPlugin.RESOURCE_FOLDER);

    List<Class> checks = GoCheckList.checks();
    metadataLoader.addRulesByAnnotatedClass(repository, checks);

    RulesDefinitionUtils.setDefaultValuesForParameters(repository, checks, Language.GO);

    repository.done();

    if (externalIssuesSupported) {
      AbstractReportSensor.createExternalRuleRepository(context, GoVetReportSensor.LINTER_ID, GoVetReportSensor.LINTER_NAME);
      AbstractReportSensor.createExternalRuleRepository(context, GoLintReportSensor.LINTER_ID, GoLintReportSensor.LINTER_NAME);
    }
  }
}
