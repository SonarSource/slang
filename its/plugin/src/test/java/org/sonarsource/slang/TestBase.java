/*
 * SonarSource SLang
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
package org.sonarsource.slang;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.ClassRule;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Measures.ComponentWsResponse;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.measures.ComponentRequest;

import static java.util.Collections.singletonList;

public abstract class TestBase {

  public static final String PROJECT_KEY = "project";
  public static final PostRequest RESET_REQUEST = new PostRequest("/api/orchestrator/reset");

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Tests.ORCHESTRATOR;

  protected SonarScanner getSonarScanner(String directoryToScan, String languageKey) {
    return getSonarScanner(directoryToScan, languageKey, null);
  }

  protected SonarScanner getSonarScanner(String directoryToScan, String languageKey, @Nullable String profileName) {
    newWsClient().wsConnector().call(RESET_REQUEST);
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
    if (profileName != null) {
      ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, languageKey, profileName);
    }
    return SonarScanner.create()
      .setProjectDir(new File(directoryToScan, languageKey))
      .setProjectKey(PROJECT_KEY)
      .setProjectName(PROJECT_KEY)
      .setProjectVersion("1")
      .setSourceDirs(".");
  }

  protected Measure getMeasure(String metricKey) {
    return getMeasure(null, metricKey);
  }

  protected Measure getMeasure(@Nullable String componentKey, String metricKey) {
    String component;
    if (componentKey != null) {
      component = PROJECT_KEY + ":" + componentKey;
    } else {
      component = PROJECT_KEY;
    }
    ComponentWsResponse response = newWsClient().measures().component(new ComponentRequest()
      .setComponent(component)
      .setMetricKeys(singletonList(metricKey)));
    List<Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }  
  
  protected Map<String, Measure> getMeasures(String projectKey, String... metricKeys) {
    return newWsClient().measures().component(new ComponentRequest()
      .setComponent(projectKey)
      .setMetricKeys(Arrays.asList(metricKeys)))
      .getComponent().getMeasuresList()
      .stream()
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  protected List<Issues.Issue> getIssuesForRule(String rule) {
    return newWsClient().issues().search(new SearchRequest().setRules(Collections.singletonList(rule))).getIssuesList();
  }

  protected Integer getMeasureAsInt(String metricKey) {
    return getMeasureAsInt(null, metricKey);
  }

  protected Integer getMeasureAsInt(@Nullable String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : Integer.parseInt(measure.getValue());
  }

  protected static WsClient newWsClient() {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(ORCHESTRATOR.getServer().getUrl())
      .build());
  }

}
