/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.slang.testing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.resources.Language;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractSensorTestTest {

  private static final SensorTester SENSOR = new SensorTester();
  private static final Language LANGUAGE = new Language() {
    @Override
    public String getKey() {
      return "dummyLang";
    }

    @Override
    public String getName() {
      return "Dummy";
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[] {".dummy"};
    }
  };

  @BeforeEach
  void setup(@TempDir File tmpBaseDir) throws Exception {
    SENSOR.logTester.beforeEach(null);
    SENSOR.setup(tmpBaseDir);
  }

  @Test
  void checkFactory_should_contain_rules() {
    SENSOR.checkFactory("S1", "S2", "S3");
    Collection<ActiveRule> rules = SENSOR.context.activeRules().findAll();
    assertThat(rules)
      .hasSize(3)
      .allMatch(rule ->  rule.ruleKey().rule().matches("S[1-3]"))
      .allMatch(rule ->  rule.ruleKey().repository().equals("myRepo"));
  }

  @Test
  void checkFactory_can_be_empty() {
    SENSOR.checkFactory();
    Collection<ActiveRule> rules = SENSOR.context.activeRules().findAll();
    assertThat(rules).isEmpty();
  }

  @Test
  void createInputFile_returns_input_file_with_same_content() throws Exception {
    String content = "class A { }";
    String filename = "yolo.dummy";

    InputFile inputFile = SENSOR.createInputFile(filename, content);

    assertThat(inputFile.contents()).isEqualTo(content);
    assertThat(inputFile.charset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(inputFile.language()).isEqualTo(LANGUAGE.getKey());
    assertThat(inputFile.type()).isEqualTo(InputFile.Type.MAIN);
    assertThat(inputFile.filename()).isEqualTo(filename);
  }

  @Test
  void createInputFile_with_status_returns_an_input_file_with_the_expected_status() throws IOException {
    String content = "class A { }";
    String filename = "yolo.dummy";

    InputFile inputFile = SENSOR.createInputFile(filename, content, InputFile.Status.ADDED);

    assertThat(inputFile.contents()).isEqualTo(content);
    assertThat(inputFile.charset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(inputFile.language()).isEqualTo(LANGUAGE.getKey());
    assertThat(inputFile.type()).isEqualTo(InputFile.Type.MAIN);
    assertThat(inputFile.filename()).isEqualTo(filename);
    assertThat(inputFile.status()).isEqualTo(InputFile.Status.ADDED);
  }

  private static class SensorTester extends AbstractSensorTest {

    @Override
    protected String repositoryKey() {
      return "myRepo";
    }

    @Override
    protected Language language() {
      return LANGUAGE;
    }
  }
}
