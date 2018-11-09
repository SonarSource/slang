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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class SlangRulingTest {

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  private static Orchestrator orchestrator;
  private static boolean keepSonarqubeRunning = "true".equals(System.getProperty("keepSonarqubeRunning"));

  private static final Set<String> LANGUAGES = ImmutableSet.of("kotlin" , "ruby" , "scala");

  @BeforeClass
  public static void setUp() {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.6"));

    addLanguagePlugins(builder);

    orchestrator = builder.build();
    orchestrator.start();

    ProfileGenerator.RulesConfiguration kotlinRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    kotlinRulesConfiguration.add("S1451", "headerFormat", "/\\*\n \\* Copyright \\d{4}-\\d{4} JetBrains s\\.r\\.o\\.");
    kotlinRulesConfiguration.add("S1451", "isRegularExpression", "true");

    ProfileGenerator.RulesConfiguration rubyRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    rubyRulesConfiguration.add("S1451", "headerFormat", "# Copyright 201\\d Twitch Interactive, Inc.  All Rights Reserved.");
    rubyRulesConfiguration.add("S1451", "isRegularExpression", "true");
    rubyRulesConfiguration.add("S1479", "maximum", "10");

    ProfileGenerator.RulesConfiguration scalaRulesConfiguration = new ProfileGenerator.RulesConfiguration();
    scalaRulesConfiguration.add("S1451", "headerFormat", "^(?i).*copyright");
    scalaRulesConfiguration.add("S1451", "isRegularExpression", "true");

    File kotlinProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "kotlin", "kotlin", kotlinRulesConfiguration, Collections.emptySet());
    File rubyProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "ruby", "ruby", rubyRulesConfiguration, Collections.emptySet());
    File scalaProfile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "scala", "scala", scalaRulesConfiguration, Collections.emptySet());
    orchestrator.getServer().restoreProfile(FileLocation.of(kotlinProfile));
    orchestrator.getServer().restoreProfile(FileLocation.of(rubyProfile));
    orchestrator.getServer().restoreProfile(FileLocation.of(scalaProfile));
  }

  private static void addLanguagePlugins(OrchestratorBuilder builder) {
    String slangVersion = System.getProperty("slangVersion");

    LANGUAGES.forEach(language -> {
      Location pluginLocation;
      String plugin = "sonar-" + language +"-plugin";
      if (StringUtils.isEmpty(slangVersion)) {
        // use the plugin that was built on local machine
        pluginLocation = FileLocation.byWildcardMavenFilename(new File("../../" + plugin + "/target"), plugin + "-*.jar");
      } else {
        // QA environment downloads the plugin built by the CI job
        pluginLocation = MavenLocation.of("org.sonarsource.slang", plugin, slangVersion);
      }

      builder.addPlugin(pluginLocation);
    });
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void kotlin_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_kotlin();
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void ruby_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_ruby();
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void scala_manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test_scala();
  }

  @Test
  public void test_kotlin() throws IOException {
    run_ruling_test("kotlin", ImmutableMap.of(
      "sonar.inclusions", "sources/kotlin/**/*.kt, ruling/src/test/resources/sources/kotlin/**/*.kt",
      "sonar.exclusions", "**/testData/**/*"));
  }

  @Test
  public void test_ruby() throws IOException {
    run_ruling_test("ruby", ImmutableMap.of(
      "sonar.inclusions", "sources/ruby/**/*.rb, ruling/src/test/resources/sources/ruby/**/*.rb"));
  }

  @Test
  public void test_scala() throws IOException {
    run_ruling_test("scala", ImmutableMap.of(
      "sonar.inclusions", "sources/scala/**/*.scala, ruling/src/test/resources/sources/scala/**/*.scala"));
  }

  private void run_ruling_test(String language, Map<String, String> languageProperties) throws IOException {
    Map<String, String> properties = new HashMap<>(languageProperties);
    properties.put("sonar.slang.converter.validation", "log");

    String projectKey = language + "-project";
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, language, "rules");

    File actualDirectory = FileLocation.of("target/actual/" + language).getFile();
    actualDirectory.mkdirs();

    File litsDifferencesFile = FileLocation.of("target/" + language + "-differences").getFile();
    SonarScanner build = SonarScanner.create(FileLocation.of("../").getFile())
      .setProjectKey(projectKey)
      .setProjectName(projectKey)
      .setProjectVersion("1")
      .setSourceDirs("./")
      .setSourceEncoding("utf-8")
      .setProperties(properties)
      .setProperty("dump.old", FileLocation.of("src/test/resources/expected/" + language).getFile().getAbsolutePath())
      .setProperty("dump.new", actualDirectory.getAbsolutePath())
      .setProperty("lits.differences", litsDifferencesFile.getAbsolutePath())
      .setProperty("sonar.cpd.skip", "true")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.language", language)
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m");

    if (!keepSonarqubeRunning) {
      build.setProperty("sonar.analysis.mode", "preview");
    }

    orchestrator.executeBuild(build);

    String litsDifference = new String(Files.readAllBytes(litsDifferencesFile.toPath()));
    assertThat(litsDifference).isEmpty();
  }

  @AfterClass
  public static void after() {
    if (keepSonarqubeRunning) {
      // keep server running, use CTRL-C to stop it
      new Scanner(System.in).next();
    }
  }

}
