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

import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonarsource.slang.testing.AbstractSensorTest;

import static org.assertj.core.api.Assertions.assertThat;

class RubySensorTest extends AbstractSensorTest {

  @Test
  void simple_file() {
    InputFile inputFile = createInputFile("file1.rb", "" +
      "class C\nend\nputs '1 == 1'; puts 'abc'\n");
    context.fileSystem().add(inputFile);
    sensor(checkFactory()).execute(context);

    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).containsExactly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 5)).isEmpty();

    // FIXME
    //assertThat(logTester.logs()).contains("1 source files to be analyzed");
  }

  @Test
  void test_fail_parsing() {
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


  @Override
  protected String repositoryKey() {
    return RubyPlugin.RUBY_REPOSITORY_KEY;
  }

  @Override
  protected RubyLanguage language() {
    return new RubyLanguage(new MapSettings().asConfig());
  }

  private RubySensor sensor(CheckFactory checkFactory) {
    return new RubySensor(SQ_LTS_RUNTIME, checkFactory, fileLinesContextFactory, new DefaultNoSonarFilter(), language());
  }

}
