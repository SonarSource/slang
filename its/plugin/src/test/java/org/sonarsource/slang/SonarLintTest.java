/*
 * SonarSource SLang
 * Copyright (C) 2018-2023 SonarSource SA
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
import com.sonar.orchestrator.locator.Locators;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.Language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static StandaloneSonarLintEngine sonarlintEngine;

  private static File baseDir;

  @BeforeClass
  public static void prepare() throws Exception {
    // Orchestrator is used only to retrieve plugin artifacts from filesystem or maven
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv();
    Tests.addLanguagePlugins(orchestratorBuilder);
    Orchestrator orchestrator = orchestratorBuilder
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(Tests.SQ_VERSION_PROPERTY, Tests.DEFAULT_SQ_VERSION))
      .build();

    Locators locators = orchestrator.getConfiguration().locators();
    StandaloneGlobalConfiguration.Builder sonarLintConfigBuilder = StandaloneGlobalConfiguration.builder();
    orchestrator.getDistribution().getPluginLocations().stream()
      .filter(location -> !location.toString().contains("sonar-reset-data-plugin"))
      .map(plugin -> locators.locate(plugin).toPath())
      .forEach(sonarLintConfigBuilder::addPlugin);

    sonarLintConfigBuilder
      .setSonarLintUserHome(temp.newFolder().toPath())
      .setLogOutput((formattedMessage, level) -> {
        /* Don't pollute logs */
      });
    StandaloneGlobalConfiguration configuration = sonarLintConfigBuilder
      .addEnabledLanguage(Language.RUBY)
      .addEnabledLanguage(Language.SCALA)
      .addEnabledLanguage(Language.GO)
      .build();
    sonarlintEngine = new StandaloneSonarLintEngineImpl(configuration);
    baseDir = temp.newFolder();
  }

  @AfterClass
  public static void stop() {
    sonarlintEngine.stop();
  }

  @Test
  public void test_ruby() throws Exception {
    ClientInputFile inputFile = prepareInputFile("foo.rb",
      "def fooBar() \n"
        + "  if true \n"
        + "    password = 'blabla' \n"
        + "  end \n"
        + "end \n",
      false, "ruby");

    List<Issue> issues = new ArrayList<>();
    StandaloneAnalysisConfiguration analysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFiles(Collections.singletonList(inputFile))
      .build();
    sonarlintEngine.analyze(analysisConfiguration, issues::add, null, null);

    assertThat(issues).extracting(Issue::getRuleKey, Issue::getStartLine, issue -> issue.getInputFile().getPath(), Issue::getSeverity)
      .containsExactlyInAnyOrder(
        tuple("ruby:S100", 1, inputFile.getPath(), IssueSeverity.MINOR),
        tuple("ruby:S1145", 2, inputFile.getPath(), IssueSeverity.MAJOR),
        tuple("ruby:S1481", 3, inputFile.getPath(), IssueSeverity.MINOR)
      );
  }

  @Test
  public void test_scala() throws Exception {
    ClientInputFile inputFile = prepareInputFile("foo.scala",
      "object Code {\n" +
        "  def foo_bar() = {\n" + // scala:S100 (Method name)
        "    if (true) { \n" + // scala:S1145 (Useless if(true))
        "        val password = \"blabla\"\n" +  // scala:S181 (Unused variable)
        "    } \n" +
        "  }\n" +
        "}",
      false, "scala");

    List<Issue> issues = new ArrayList<>();
    StandaloneAnalysisConfiguration analysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFiles(Collections.singletonList(inputFile))
      .build();
    sonarlintEngine.analyze(analysisConfiguration, issues::add, null, null);

    assertThat(issues).extracting(Issue::getRuleKey, Issue::getStartLine, issue -> issue.getInputFile().getPath(), Issue::getSeverity)
      .containsExactlyInAnyOrder(
        tuple("scala:S100", 2, inputFile.getPath(), IssueSeverity.MINOR),
        tuple("scala:S1145", 3, inputFile.getPath(), IssueSeverity.MAJOR),
        tuple("scala:S1481", 4, inputFile.getPath(), IssueSeverity.MINOR)
      );
  }

  @Test
  public void test_go() throws Exception {
    ClientInputFile inputFile = prepareInputFile("foo.go",
      "package main\n" +
        "func empty() {\n" +  // go:S1186 (empty function)
        "}\n",
      false, "go");

    List<Issue> issues = new ArrayList<>();
    StandaloneAnalysisConfiguration analysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFiles(Collections.singletonList(inputFile))
      .build();
    sonarlintEngine.analyze(analysisConfiguration, issues::add, null, null);

    assertThat(issues).extracting(Issue::getRuleKey, Issue::getStartLine, issue -> issue.getInputFile().getPath(), Issue::getSeverity)
      .containsExactlyInAnyOrder(
        tuple("go:S1186", 2, inputFile.getPath(), IssueSeverity.CRITICAL)
      );
  }

  private ClientInputFile prepareInputFile(String relativePath, String content, final boolean isTest, String language) throws IOException {
    File file = new File(baseDir, relativePath);
    FileUtils.write(file, content, StandardCharsets.UTF_8);
    return createInputFile(file.toPath(), isTest, language);
  }

  private ClientInputFile createInputFile(final Path path, final boolean isTest, String language) {
    return new ClientInputFile() {

      @Override
      public URI uri() {
        return path.toUri();
      }

      @Override
      public String getPath() {
        return path.toString();
      }

      @Override
      public boolean isTest() {
        return isTest;
      }

      @Override
      public Charset getCharset() {
        return StandardCharsets.UTF_8;
      }


      @Override
      public <G> G getClientObject() {
        return null;
      }

      @Override
      public String contents() throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
      }

      @Override
      public String relativePath() {
        return path.toString();
      }

      @Override
      public InputStream inputStream() throws IOException {
        return Files.newInputStream(path);
      }

      @CheckForNull
      @Override
      public Language language() {
        return Language.forKey(language).orElse(Language.APEX);
      }
    };
  }

}
