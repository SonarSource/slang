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
package org.sonarsource.slang.externalreport.detekt;

import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class DetektRulesDefinitionTest {

  @Test
  public void external_repositories_not_supported() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    DetektRulesDefinition rulesDefinition = new DetektRulesDefinition(false);
    rulesDefinition.define(context);
    assertThat(context.repositories()).isEmpty();
  }

  @Test
  public void detekt_external_repository() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    DetektRulesDefinition rulesDefinition = new DetektRulesDefinition(true);
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository("external_detekt");
    assertThat(repository.name()).isEqualTo("detekt");
    assertThat(repository.language()).isEqualTo("kotlin");
    assertThat(repository.isExternal()).isEqualTo(true);

    assertThat(repository.rules().size()).isEqualTo(111);

    RulesDefinition.Rule classNaming = repository.rule("ClassNaming");
    assertThat(classNaming).isNotNull();
    assertThat(classNaming.name()).isEqualTo("Class Naming");
    assertThat(classNaming.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(classNaming.severity()).isEqualTo("INFO");
    assertThat(classNaming.htmlDescription()).isEqualTo("<p>A classes name should fit the naming pattern defined in the projects configuration.</p> " +
      "<p>See more at <a href=\"https://arturbosch.github.io/detekt/naming.html#classnaming\">detekt website</a>.</p>");
    assertThat(classNaming.tags()).containsExactlyInAnyOrder("style");
    assertThat(classNaming.debtRemediationFunction().baseEffort()).isEqualTo("5min");
  }

}
