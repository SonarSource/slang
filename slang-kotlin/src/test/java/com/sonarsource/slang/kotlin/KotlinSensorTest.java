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
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;

public class KotlinSensorTest {

  private File baseDir = new File("src/test/resources/sensor");
  private SensorContextTester context = SensorContextTester.create(baseDir);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_one_rule() throws Exception {
    InputFile inputFile = addInputFile("file1.kt", "" +
      "fun main(args: Array<String>) {\nprint (1 == 1);}");
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

  private void assertTextRange(TextRange textRange, int startLine, int startLineOffset, int endLine, int endLineOffset) {
    assertThat(textRange.start().line()).isEqualTo(startLine);
    assertThat(textRange.start().lineOffset()).isEqualTo(startLineOffset);
    assertThat(textRange.end().line()).isEqualTo(endLine);
    assertThat(textRange.end().lineOffset()).isEqualTo(endLineOffset);
  }

  private InputFile addInputFile(String relativePath, String content) {
    DefaultInputFile inputFile = new TestInputFileBuilder("moduleKey", relativePath)
      .setModuleBaseDir(baseDir.toPath())
      .setType(InputFile.Type.MAIN)
      .setLanguage(KotlinPlugin.LANGUAGE_KEY)
      .setCharset(StandardCharsets.UTF_8)
      .setContents(content)
      .build();

    context.fileSystem().add(inputFile);

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
