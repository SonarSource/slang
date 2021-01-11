/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class ScalastyleRulesDefinitionTest {

  @Test
  public void scalastyle_external_repository() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    ScalastyleRulesDefinition rulesDefinition = new ScalastyleRulesDefinition();
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository scalastyleRepository = context.repository("external_scalastyle");
    assertThat(scalastyleRepository.name()).isEqualTo("Scalastyle");
    assertThat(scalastyleRepository.language()).isEqualTo("scala");
    assertThat(scalastyleRepository.isExternal()).isTrue();
    assertThat(scalastyleRepository.rules().size()).isEqualTo(72);

    RulesDefinition.Rule rule = scalastyleRepository.rule("org.scalastyle.file.FileLengthChecker");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("File size limit");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.severity()).isEqualTo("MINOR");
    assertThat(rule.htmlDescription()).isEqualTo("" +
      "See description of Scalastyle rule <code>org.scalastyle.file.FileLengthChecker</code> at the <a href=\"http://www.scalastyle.org/rules-1.0.0.html#org_scalastyle_file_FileLengthChecker\">Scalastyle website</a>.");
    assertThat(rule.tags()).isEmpty();
    assertThat(rule.debtRemediationFunction().baseEffort()).isEqualTo("5min");
  }

}
