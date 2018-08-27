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
package org.sonarsource.ruby.plugin;

import org.junit.Test;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Param;
import org.sonar.api.server.rule.RulesDefinition.Rule;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class RubyRulesDefinitionTest {

  @Test
  public void rules() {
    RulesDefinition rulesDefinition = new RubyRulesDefinition(SonarRuntimeImpl.forSonarQube(Version.create(7,2), SonarQubeSide.SERVER));
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    RulesDefinition.Repository repository = context.repository("ruby");
    assertThat(repository.name()).isEqualTo("SonarAnalyzer");
    assertThat(repository.language()).isEqualTo("ruby");

    Rule rule = repository.rule("S1135");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Track uses of \"TODO\" tags");
    assertThat(rule.debtRemediationFunction()).isNull();
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);

    Rule ruleWithConfig = repository.rule("S100");
    Param param = ruleWithConfig.param("format");
    assertThat(param.defaultValue()).isEqualTo("^(@{0,2}[\\da-z_]+[!?=]?)|([*+-/%=!><~]+)|(\\[]=?)$");
  }

}
