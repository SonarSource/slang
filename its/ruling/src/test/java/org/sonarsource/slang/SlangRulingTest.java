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
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Scanner;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class SlangRulingTest {

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "7.2";

  private static Orchestrator orchestrator;
  private static boolean keepSonarqubeRunning = false;

  @BeforeClass
  public static void setUp() {
    String slangVersion = System.getProperty("slangVersion");
    Location slangLocation;
    if (StringUtils.isEmpty(slangVersion)) {
      // use the plugin that was built on local machine
      slangLocation = FileLocation.byWildcardMavenFilename(new File("../../sonar-kotlin-plugin/target"), "sonar-kotlin-plugin-*.jar");
    } else {
      // QA environment downloads the plugin built by the CI job
      slangLocation = MavenLocation.of("org.sonarsource.slang", "sonar-kotlin-plugin", slangVersion);
    }

    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.6"))
      .addPlugin(slangLocation);

    orchestrator = builder.build();
    orchestrator.start();
    ProfileGenerator.RulesConfiguration rulesConfiguration = new ProfileGenerator.RulesConfiguration();
    rulesConfiguration.add("S1451", "headerFormat", "/\\*\n \\* Copyright \\d{4}-\\d{4} JetBrains s\\.r\\.o\\.");
    rulesConfiguration.add("S1451", "isRegularExpression", "true");
    File profile = ProfileGenerator.generateProfile(SlangRulingTest.orchestrator.getServer().getUrl(), "kotlin", "kotlin", rulesConfiguration, Collections.emptySet());
    orchestrator.getServer().restoreProfile(FileLocation.of(profile));
  }

  @Test
  // @Ignore because it should only be run manually
  @Ignore
  public void manual_keep_sonarqube_server_up() throws IOException {
    keepSonarqubeRunning = true;
    test();
  }

  @Test
  public void test() throws IOException {
    orchestrator.getServer().provisionProject("kotlin-project", "kotlin-project");
    orchestrator.getServer().associateProjectToQualityProfile("kotlin-project", "kotlin", "rules");

    File litsDifferencesFile = FileLocation.of("target/differences").getFile();
    SonarScanner build = SonarScanner.create(FileLocation.of("../sources/kotlin").getFile())
      .setProjectKey("kotlin-project")
      .setProjectName("kotlin-project")
      .setProjectVersion("1")
      .setSourceDirs("./")
      .setSourceEncoding("utf-8")
      .setProperty("sonar.inclusions", "**/*.kt")
      .setProperty("sonar.exclusions", "**/testData/**/*")
      .setProperty("dump.old", FileLocation.of("src/test/resources/expected").getFile().getAbsolutePath())
      .setProperty("dump.new", FileLocation.of("target/actual").getFile().getAbsolutePath())
      .setProperty("lits.differences", litsDifferencesFile.getAbsolutePath())
      .setProperty("sonar.cpd.skip", "true")
      .setProperty("sonar.scm.disabled", "true")
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
