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
package org.sonarsource.scala.plugin;

import java.util.Collection;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonarsource.slang.testing.AbstractSensorTest;

import static org.assertj.core.api.Assertions.assertThat;

public class ScalaSensorTest extends AbstractSensorTest {

  @Test
  public void test_fail_parsing() {
    InputFile inputFile = createInputFile("file1.scala", "invalid scala source code");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("ParsingError");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.scala");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isNotNull();
    assertThat(textPointer.line()).isEqualTo(1);
    assertThat(textPointer.lineOffset()).isEqualTo(0);

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. Parse error at position 1:0", inputFile.uri()));
  }

  @Test
  public void token_validation_map_is_not_empty() {
    ScalaSensor sensor = sensor(checkFactory("ParsingError"));
    assertThat(sensor.tokenValidationMap()).isNotEmpty();
  }

  @Override
  protected String repositoryKey() {
    return ScalaPlugin.SCALA_REPOSITORY_KEY;
  }

  @Override
  protected ScalaLanguage language() {
    return new ScalaLanguage(new MapSettings().asConfig());
  }

  private ScalaSensor sensor(CheckFactory checkFactory) {
    return new ScalaSensor(checkFactory, fileLinesContextFactory, new NoSonarFilter(), language());
  }

}
