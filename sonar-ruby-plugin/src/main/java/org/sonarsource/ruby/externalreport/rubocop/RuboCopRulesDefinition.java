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
package org.sonarsource.ruby.externalreport.rubocop;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;
import org.sonarsource.ruby.plugin.RubyPlugin;

import static org.sonarsource.ruby.externalreport.rubocop.RuboCopSensor.LINTER_KEY;
import static org.sonarsource.ruby.externalreport.rubocop.RuboCopSensor.LINTER_NAME;

public class RuboCopRulesDefinition implements RulesDefinition {

  private static final String RULES_JSON = "org/sonar/l10n/ruby/rules/rubocop/rules.json";

  private static final String RULE_REPOSITORY_LANGUAGE = RubyPlugin.RUBY_LANGUAGE_KEY;

  static final ExternalRuleLoader RULE_LOADER = new ExternalRuleLoader(LINTER_KEY, LINTER_NAME, RULES_JSON, RULE_REPOSITORY_LANGUAGE);

  private final boolean externalIssuesSupported;

  public RuboCopRulesDefinition(boolean externalIssuesSupported) {
    this.externalIssuesSupported = externalIssuesSupported;
  }

  @Override
  public void define(Context context) {
    if (externalIssuesSupported) {
      RULE_LOADER.createExternalRuleRepository(context);
    }
  }

}
