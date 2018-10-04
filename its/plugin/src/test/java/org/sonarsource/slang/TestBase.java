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
package org.sonarsource.slang;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.ClassRule;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.ws.Measures.ComponentWsResponse;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
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

  protected List<Issue> getIssuesForRule(String rule) {
    Server server = ORCHESTRATOR.getServer();
    IssueClient issueClient = SonarClient.create(server.getUrl()).issueClient();
    return issueClient.find(IssueQuery.create().componentRoots(PROJECT_KEY).rules(rule)).list();
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
