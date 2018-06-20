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
package com.sonarsource.slang;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultProfileTest extends TestBase {

  @Test
  public void kotlin_profiles() {
    List<QualityProfile> profilesList = newWsClient().qualityProfiles().search(new SearchWsRequest().setLanguage("kotlin")).getProfilesList();
    assertThat(profilesList).extracting(QualityProfile::getName).containsExactlyInAnyOrder("Sonar way", "norule-profile", "nosonar-profile");

    Map<String, String> profileMap = profilesList.stream().collect(Collectors.toMap(QualityProfile::getName, QualityProfile::getKey));
    String sonarWayProfileKey = profileMap.get("Sonar way");
    String noSonarProfileKey = profileMap.get("nosonar-profile");

    Map<String, Rules.ActiveList> activeRulesByRuleKey = newWsClient().rules().search(
      new org.sonarqube.ws.client.rule.SearchWsRequest()
        .setLanguages(Collections.singletonList("kotlin"))
        .setRepositories(Collections.singletonList("kotlin"))
        .setFields(Collections.singletonList("actives")))
      .getActives().getActives();

    assertThat(getProfiles(activeRulesByRuleKey.get("kotlin:S103"))).containsExactly();
    assertThat(getProfiles(activeRulesByRuleKey.get("kotlin:S1134"))).containsExactlyInAnyOrder(sonarWayProfileKey);
    assertThat(getProfiles(activeRulesByRuleKey.get("kotlin:S100"))).containsExactlyInAnyOrder(sonarWayProfileKey, noSonarProfileKey);
  }

  private Set<String> getProfiles(Rules.ActiveList activeProfileList) {
    int activeListCount = activeProfileList.getActiveListCount();
    Set<String> profileKeys = new HashSet<>();
    for (int i = 0; i < activeListCount; i++) {
      profileKeys.add(activeProfileList.getActiveList(i).getQProfile());
    }
    return profileKeys;
  }

}
