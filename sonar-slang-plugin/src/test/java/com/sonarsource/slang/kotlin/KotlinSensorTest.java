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
package com.sonarsource.slang.kotlin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class KotlinSensorTest {

  private File baseDir = new File("src/test/resources/sensor");
  private SensorContextTester context;

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void setup() {
    context = SensorContextTester.create(baseDir);
  }

  @Test
  public void test_one_rule() {
    InputFile inputFile = createInputFile("file1.kt", "" +
      "fun main(args: Array<String>) {\nprint (1 == 1);}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S1764");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Correct one of the identical sub-expressions on both sides this operator");
    assertTextRange(location.textRange(), 2, 12, 2, 13);
  }

  @Test
  public void test_rule_with_gap() {
    InputFile inputFile = createInputFile("file1.kt", "" +
      "fun f() { print(\"string literal\"); print(\"string literal\"); print(\"string literal\"); }");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1192");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S1192");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Define a constant instead of duplicating this literal \"string literal\" 3 times.");
    assertTextRange(location.textRange(), 1, 16, 1, 32);
    assertThat(issue.gap()).isEqualTo(2.0);
  }

  @Test
  public void test_fail_input() throws IOException {
    InputFile inputFile = createInputFile("fakeFile.kt", "");
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(spyInputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(spyInputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: fakeFile.kt");
    assertThat(analysisError.location()).isNull();

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. ", inputFile.uri()));
  }

  @Test
  public void test_fail_parsing() {
    InputFile inputFile = createInputFile("file1.kt", "" +
      "enum class A { <!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!> }");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.kt");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isNotNull();
    assertThat(textPointer.line()).isEqualTo(1);
    assertThat(textPointer.lineOffset()).isEqualTo(14);

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. Parse error at position 1:14", inputFile.uri()));
  }

  private void assertTextRange(TextRange textRange, int startLine, int startLineOffset, int endLine, int endLineOffset) {
    assertThat(textRange.start().line()).isEqualTo(startLine);
    assertThat(textRange.start().lineOffset()).isEqualTo(startLineOffset);
    assertThat(textRange.end().line()).isEqualTo(endLine);
    assertThat(textRange.end().lineOffset()).isEqualTo(endLineOffset);
  }

  private InputFile createInputFile(String relativePath, String content) {
    DefaultInputFile inputFile = new TestInputFileBuilder("moduleKey", relativePath)
      .setModuleBaseDir(baseDir.toPath())
      .setType(InputFile.Type.MAIN)
      .setLanguage(KotlinPlugin.LANGUAGE_KEY)
      .setCharset(StandardCharsets.UTF_8)
      .setContents(content)
      .build();

    return inputFile;
  }

  private CheckFactory checkFactory(String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String ruleKey : ruleKeys) {
      builder.create(RuleKey.of(KotlinPlugin.REPOSITORY_KEY, ruleKey))
        .setName(ruleKey)
        .activate();
    }
    context.setActiveRules(builder.build());
    return new CheckFactory(context.activeRules());
  }

  private KotlinSensor sensor(CheckFactory checkFactory) {
    return new KotlinSensor(checkFactory);
  }

}
