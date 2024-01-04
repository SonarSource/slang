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
package org.sonarsource.slang.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonarsource.slang.parser.SLangConverter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SkipNoSonarLinesVisitorTest {

  private File tempFolder;
  private SLangConverter sLangConverter;

  private NoSonarFilter mockNoSonarFilter;
  private SkipNoSonarLinesVisitor visitor;

  @BeforeEach
  void setUp(@TempDir File tempFolder) {
    this.tempFolder = tempFolder;
    this.sLangConverter = new SLangConverter();
    mockNoSonarFilter = mock(NoSonarFilter.class);
    visitor = new SkipNoSonarLinesVisitor(mockNoSonarFilter);
  }

  @Test
  void testNoDeclarations() throws Exception {
    testNosonarCommentLines("// NOSONAR comment\n", Set.of());
  }

  @Test
  void testSingleNosonarComment() throws Exception {
    testNosonarCommentLines("import something; \n"
        + "// NOSONAR comment\n"
        + "fun function1() { // comment\n"
        + "x = true || false; }",
      Set.of(2));
  }

  @Test
  void testMultipleNosonarComments() throws IOException {
    testNosonarCommentLines("/* File Header */"
        + "import something; \n"
        + "fun foo() { // NOSONAR\n"
        + "  // comment\n"
        + "}\n"
        + "\n"
        + "fun bar() {\n"
        + "  // nosonar\n"
        + "  foo();\n"
        + "}",
      Set.of(2, 7));
  }

  private void testNosonarCommentLines(String content, Set<Integer> expectedNosonarCommentLines) throws IOException {
    InputFile inputFile = createInputFile(content);

    visitor.scan(createInputFileContext(inputFile), sLangConverter.parse(content));

    verify(mockNoSonarFilter).noSonarInFile(inputFile, expectedNosonarCommentLines);
  }

  private InputFile createInputFile(String content) throws IOException {
    File file = File.createTempFile("file", ".tmp", tempFolder);
    return new TestInputFileBuilder("moduleKey", file.getName())
      .setContents(content)
      .build();
  }

  private InputFileContext createInputFileContext(InputFile inputFile){
    SensorContextTester sensorContext = SensorContextTester.create(tempFolder);
    return new InputFileContext(sensorContext, inputFile);
  }
}