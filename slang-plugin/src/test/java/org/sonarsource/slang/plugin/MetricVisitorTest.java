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
package org.sonarsource.slang.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonarsource.slang.parser.SLangConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricVisitorTest {

  private File tempFolder;
  private SLangConverter parser = new SLangConverter();
  private MetricVisitor visitor;
  private SensorContextTester sensorContext;
  private DefaultInputFile inputFile;

  @BeforeEach
  void setUp(@TempDir File tempFolder) {
    this.tempFolder = tempFolder;
    sensorContext = SensorContextTester.create(tempFolder);
    FileLinesContext mockFileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory mockFileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(mockFileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(mockFileLinesContext);
    visitor = new MetricVisitor(mockFileLinesContextFactory, SlangSensor.EXECUTABLE_LINE_PREDICATE);
  }

  @Test
  void emptySource() throws Exception {
    scan("");
    assertThat(visitor.linesOfCode()).isEmpty();
    assertThat(visitor.commentLines()).isEmpty();
    assertThat(visitor.numberOfFunctions()).isZero();
  }

  @Test
  void linesOfCode() throws Exception {
    scan("" +
      "x + 1;\n" +
      "// comment\n" +
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.linesOfCode()).containsExactly(1, 3, 4);
  }

  @Test
  void commentLines() throws Exception {
    scan("" +
      "x + 1;\n" +
      "// comment\n" +
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.commentLines()).containsExactly(2, 3);
  }

  @Test
  void commentBeforeTheFirstTokenCorrespondToTheIgnoredHeader() throws Exception {
    scan("" +
      "// first line of the header\n" +
      "// second line of the header\n" +
      "/*\n" +
      "  this is also part of the header\n" +
      "*/\n" +
      "package abc; // comment 1\n" +
      "import x;\n" +
      "\n" +
      "fun function1() { // comment 2\n" +
      "  //\n" +
      "  /**/\n" +
      "}\n");
    assertThat(visitor.commentLines()).containsExactly(6, 9);
  }

  @Test
  void commentsWithoutDeclarationsAreIgnored() throws Exception {
    scan("" +
      "// header 1\n" +
      "/**\n" +
      " * header 2\n" +
      " */\n");
    assertThat(visitor.commentLines()).isEmpty();
  }

  @Test
  void noSonarCommentsDoNotAccountForTheCommentMetrics() throws Exception {
    scan("" +
      "fun function1() {\n" +
      "  // comment1\n" +
      "  // NOSONAR comment2\n" +
      "  // comment3\n" +
      "}\n");
    assertThat(visitor.commentLines()).containsExactly(2, 4);
  }

  @Test
  void emptyLinesDoNotAccountForTheCommentMetrics() throws Exception {
    scan("" +
      "package abc; // comment 1\n" +
      "/*\n" +
      "\n" +
      "  comment 2\n" +
      "  \n" +
      "  comment 3\n" +
      "\n" +
      "*/\n" +
      "\n" +
      "fun function1() { // comment 4\n" +
      "  /**\n" +
      "   *\n"+
      "   #\n"+
      "   =\n"+
      "   -\n"+
      "   |\n"+
      "   | comment 5\n"+
      "   | どのように\n"+
      "   |\n"+
      "   */\n"+
      "}\n");
    assertThat(visitor.commentLines()).containsExactlyInAnyOrder(1, 4, 6, 10, 17, 18);
  }

  @Test
  void multiLineComment() throws Exception {
    scan("" +
      "/*start\n" +
      "x + 1\n" +
      "end*/");
    assertThat(visitor.commentLines()).containsExactly(1, 2, 3);
    assertThat(visitor.linesOfCode()).isEmpty();
  }

  @Test
  void functions() throws Exception {
    scan("" +
      "x + 1;\n" +
      "x = true || false;");
    assertThat(visitor.numberOfFunctions()).isZero();
    scan("" +
      "x + 1;\n" +
      "fun noBodyFunction();\n" + // Only functions with implementation bodies are considered for the metric
      "fun() { x = 1; }\n" + // Anonymous functions are not considered for function metric computation
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.numberOfFunctions()).isEqualTo(1);
  }

  @Test
  void classes() throws Exception {
    scan("" +
            "x + 1;\n" +
            "x = true || false;");
    assertThat(visitor.numberOfClasses()).isZero();
    scan("" +
            "class C {}\n" +
            "fun function() {}\n" +
            "class D { int val x = 0; }\n" +
            "class E {\n" +
            "  fun doSomething(int x) {}\n" +
            "}");
    assertThat(visitor.numberOfClasses()).isEqualTo(3);
  }

  @Test
  void cognitiveComplexity() throws Exception {
    scan("" +
      "class A { fun foo() { if(1 != 1) 1; } }" + // +1 for 'if'
      "fun function() {" +
      "  if (1 != 1) {" + // +1 for 'if'
      "    if (1 != 1) {" + // + 2 for nested 'if'
      "      1" +
      "    }" +
      "  };" +
      "  class B {" + // Nesting level reset here because of class declaration
      "    fun bar(int a) {" +
      "      match(a) {" + // +1 for match
      "        1 -> doSomething();" +
      "        2 -> doSomething();" +
      "        else -> if (1 != 1) doSomething();" + // +2 for nested 'if'
      "      }" +
      "    }" +
      "  };" +
      "}");
    assertThat(visitor.cognitiveComplexity()).isEqualTo(7);
  }

  @Test
  void executable_lines() throws Exception {
    scan("" +
      "package abc;\n" +
      "import x;\n" +
      "class A {\n" +
      "  fun foo() {\n" +
      "    statementOnSeveralLines(a,\n" +
      "      b);\n" +
      "  };\n" +
      "}\n" +
      "{\n" +
      "  x = 42;\n" +
      "};");
    assertThat(visitor.executableLines()).containsExactly(5, 10);
  }

  private void scan(String code) throws IOException {
    File tmpFile = File.createTempFile("file", ".tmp", tempFolder);
    inputFile = new TestInputFileBuilder("moduleKey", tmpFile.getName())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(code).build();
    InputFileContext ctx = new InputFileContext(sensorContext, inputFile);
    visitor.scan(ctx, parser.parse(code));
  }

}
