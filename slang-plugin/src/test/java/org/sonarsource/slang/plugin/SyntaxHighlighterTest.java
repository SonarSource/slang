/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonarsource.slang.parser.SLangConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.COMMENT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.CONSTANT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.KEYWORD;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.STRING;

class SyntaxHighlighterTest {

  private SyntaxHighlighter highlightingVisitor = new SyntaxHighlighter();

  private SensorContextTester sensorContext;

  private SLangConverter parser = new SLangConverter();

  private DefaultInputFile inputFile;

  private File tempFolder;

  @BeforeEach
  void setUp(@TempDir File tempFolder) {
    this.tempFolder = tempFolder;
    sensorContext = SensorContextTester.create(tempFolder);
  }

  private void highlight(String code) throws IOException {
    File tmpFile = File.createTempFile("file", ".tmp", tempFolder);
    inputFile = new TestInputFileBuilder("moduleKey", tmpFile.getName())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(code).build();
    InputFileContext ctx = new InputFileContext(sensorContext, inputFile);
    highlightingVisitor.scan(ctx, parser.parse(code));
  }

  private void assertHighlighting(int columnFirst, int columnLast, @Nullable TypeOfText type) {
    assertHighlighting(1, columnFirst, columnLast, type);
  }

  private void assertHighlighting(int line, int columnFirst, int columnLast, @Nullable TypeOfText type) {
    for (int i = columnFirst; i <= columnLast; i++) {
      List<TypeOfText> typeOfTexts = sensorContext.highlightingTypeAt(inputFile.key(), line, i);
      if (type != null) {
        assertThat(typeOfTexts).as("Expect highlighting " + type + " at line " + line + " lineOffset " + i).containsExactly(type);
      } else {
        assertThat(typeOfTexts).as("Expect no highlighting at line " + line + " lineOffset " + i).containsExactly();
      }
    }
  }

  @Test
  void empty_input() throws Exception {
    highlight("");
    assertHighlighting(1, 0, 0, null);
  }

  @Test
  void single_line_comment() throws Exception {
    highlight("  // Comment ");
    assertHighlighting(0, 1, null);
    assertHighlighting(2, 12, COMMENT);
  }

  @Test
  void comment() throws Exception {
    highlight("  /*Comment*/ ");
    assertHighlighting(0, 1, null);
    assertHighlighting(2, 12, COMMENT);
    assertHighlighting(13, 13, null);
  }

  @Test
  void multiline_comment() throws Exception {
    highlight("/*\nComment\n*/ ");
    assertHighlighting(1, 0, 1, COMMENT);
    assertHighlighting(2, 0, 6, COMMENT);
    assertHighlighting(3, 0, 1, COMMENT);
    assertHighlighting(3, 2, 2, null);
  }

  @Test
  void keyword() throws Exception {
    highlight("fun foo() { if(x) y; }");
    assertHighlighting(0, 2, KEYWORD);
    assertHighlighting(3, 11, null);
    assertHighlighting(12, 13, KEYWORD);
    assertHighlighting(14, 22, null);
  }

  @Test
  void string_literal() throws Exception {
    highlight("x + \"abc\" + y;");
    assertHighlighting(1, 3, null);
    assertHighlighting(4, 8, STRING);
    assertHighlighting(9, 9, null);
  }

  @Test
  void numeric_literal() throws Exception {
    highlight("x + 123 + y;");
    assertHighlighting(1, 3, null);
    assertHighlighting(4, 6, CONSTANT);
    assertHighlighting(7, 7, null);
  }

}
