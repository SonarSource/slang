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
package org.sonar.go.plugin;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonar.go.externalreport.ExternalKeyUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GoRulesDefinitionTest {

  private static final SonarRuntime RUNTIME = SonarRuntimeImpl.forSonarQube(Version.create(8, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);

  @Test
  void test() {
    GoRulesDefinition rulesDefinition = new GoRulesDefinition(RUNTIME);
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(3);

    RulesDefinition.Repository goRepository = context.repository("go");

    assertThat(goRepository.name()).isEqualTo("Sonar");
    assertThat(goRepository.language()).isEqualTo("go");
    assertThat(goRepository.rules()).hasSize(GoCheckList.checks().size());

    RulesDefinition.Rule rule = goRepository.rule("S4663");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Multi-line comments should not be empty");
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.activatedByDefault()).isTrue();
  }

  @Test
  void test_external_repositories() {
    GoRulesDefinition rulesDefinition = new GoRulesDefinition(RUNTIME);
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);
    RulesDefinition.Repository golintRepository = context.repository("external_golint");
    RulesDefinition.Repository govetRepository = context.repository("external_govet");

    assertThat(context.repositories()).hasSize(3);

    assertThat(golintRepository.name()).isEqualTo("Golint");
    assertThat(govetRepository.name()).isEqualTo("go vet");

    assertThat(golintRepository.language()).isEqualTo("go");
    assertThat(govetRepository.language()).isEqualTo("go");

    assertThat(golintRepository.isExternal()).isTrue();
    assertThat(govetRepository.isExternal()).isTrue();

    assertThat(golintRepository.rules()).hasSize(18);
    assertThat(ExternalKeyUtils.GO_LINT_KEYS).hasSize(18);

    assertThat(govetRepository.rules()).hasSize(21);
    assertThat(ExternalKeyUtils.GO_VET_KEYS).hasSize(21);

    List<String> govetKeysWithoutDefinition = ExternalKeyUtils.GO_VET_KEYS.stream()
      .map(x -> x.key)
      .filter(key -> govetRepository.rule(key) == null)
      .collect(Collectors.toList());
    assertThat(govetKeysWithoutDefinition).isEmpty();

    List<String> golintKeysWithoutDefinition = ExternalKeyUtils.GO_LINT_KEYS.stream()
      .map(x -> x.key)
      .filter(key -> golintRepository.rule(key) == null)
      .collect(Collectors.toList());
    assertThat(golintKeysWithoutDefinition).isEmpty();
  }

  @Test
  void owasp_security_standard_includes_2021() {
    RulesDefinition.Repository repository = getRepositoryForVersion(Version.create(9, 3));

    RulesDefinition.Rule rule = repository.rule("S1313");
    assertThat(rule).isNotNull();
    assertThat(rule.securityStandards()).containsExactlyInAnyOrder("owaspTop10:a3", "owaspTop10-2021:a1");
  }

  @Test
  void owasp_security_standard() {
    RulesDefinition.Repository repository = getRepositoryForVersion(Version.create(8, 9));

    RulesDefinition.Rule rule = repository.rule("S1313");
    assertThat(rule).isNotNull();
    assertThat(rule.securityStandards()).containsExactly("owaspTop10:a3");
  }

  private RulesDefinition.Repository getRepositoryForVersion(Version version) {
    GoRulesDefinition rulesDefinition = new GoRulesDefinition(
      SonarRuntimeImpl.forSonarQube(version, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY));
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    return context.repository("go");
  }

}
