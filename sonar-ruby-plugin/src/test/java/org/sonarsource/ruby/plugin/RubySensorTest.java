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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class RubySensorTest {

  private File baseDir = new File("src/test/resources/sensor");
  private SensorContextTester context;
  private FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void setup() {
    context = SensorContextTester.create(baseDir);
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
  }

  @Test
  public void simple_file() {
    InputFile inputFile = createInputFile("file1.rb", "" +
      "class C\nend\nputs '1 == 1'; puts 'abc'\n");
    context.fileSystem().add(inputFile);
    sensor(checkFactory()).execute(context);

    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).containsExactly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 5)).isEmpty();

    assertThat(logTester.logs()).contains("1 source files to be analyzed");
  }

  @Test
  public void test_fail_input() throws IOException {
    InputFile inputFile = createInputFile("fakeFile.rb", "");
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(spyInputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(spyInputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: fakeFile.rb");
    assertThat(analysisError.location()).isNull();

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. ", inputFile.uri()));
  }

  @Test
  public void test_fail_parsing() {
    InputFile inputFile = createInputFile("file1.rb", "{ <!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!> }");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.rb");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isNotNull();
    assertThat(textPointer.line()).isEqualTo(1);
    assertThat(textPointer.lineOffset()).isEqualTo(2);

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. Parse error at position 1:2", inputFile.uri()));
  }

  private InputFile createInputFile(String relativePath, String content) {
    return new TestInputFileBuilder("moduleKey", relativePath)
      .setModuleBaseDir(baseDir.toPath())
      .setType(InputFile.Type.MAIN)
      .setLanguage(RubyPlugin.RUBY_LANGUAGE_KEY)
      .setCharset(StandardCharsets.UTF_8)
      .setContents(content)
      .build();
  }

  private CheckFactory checkFactory(String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String ruleKey : ruleKeys) {
      builder.create(RuleKey.of(RubyPlugin.RUBY_REPOSITORY_KEY, ruleKey))
        .setName(ruleKey)
        .activate();
    }
    context.setActiveRules(builder.build());
    return new CheckFactory(context.activeRules());
  }

  private RubySensor sensor(CheckFactory checkFactory) {
    RubyLanguage language = new RubyLanguage(new MapSettings().asConfig());
    return new RubySensor(checkFactory, fileLinesContextFactory, new NoSonarFilter(), language);
  }

}
