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

import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class RuboCopRulesDefinitionTest {

  @Test
  public void external_repositories_not_supported() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    RuboCopRulesDefinition rulesDefinition = new RuboCopRulesDefinition(false);
    rulesDefinition.define(context);
    assertThat(context.repositories()).isEmpty();
  }

  @Test
  public void rubocop_lint_external_repository() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    RuboCopRulesDefinition rulesDefinition = new RuboCopRulesDefinition(true);
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository("external_rubocop");
    assertThat(repository.name()).isEqualTo("RuboCop");
    assertThat(repository.language()).isEqualTo("ruby");
    assertThat(repository.isExternal()).isEqualTo(true);
    assertThat(repository.rules().size()).isEqualTo(425);

    RulesDefinition.Rule rule = repository.rule("Lint/MultipleCompare");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Multiple Compare (Lint)");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.severity()).isEqualTo("MAJOR");
    assertThat(rule.htmlDescription()).isEqualTo("<p>Use `&amp;&amp;` operator to compare multiple value.</p> " +
      "<p>See more at the <a href=\"https://www.rubydoc.info/gems/rubocop/RuboCop/Cop/Lint/MultipleCompare\">RuboCop website</a>.</p>");
    assertThat(rule.tags()).isEmpty();
    assertThat(rule.debtRemediationFunction().baseEffort()).isEqualTo("5min");
  }

}
