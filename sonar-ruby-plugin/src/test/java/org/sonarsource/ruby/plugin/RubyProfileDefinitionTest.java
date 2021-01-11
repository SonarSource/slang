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
package org.sonarsource.ruby.plugin;

import org.junit.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;

import static org.assertj.core.api.Assertions.assertThat;

public class RubyProfileDefinitionTest {

  @Test
  public void profile() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    new RubyProfileDefinition().define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile("ruby", "Sonar way");

    assertThat(profile.rules()).extracting("repoKey").containsOnly("ruby");
    assertThat(profile.rules().size()).isGreaterThan(1);
    assertThat(profile.rules()).extracting(BuiltInActiveRule::ruleKey).contains("S1135");
  }

}
