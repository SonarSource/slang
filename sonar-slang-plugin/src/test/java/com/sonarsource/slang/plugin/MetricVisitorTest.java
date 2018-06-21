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
package com.sonarsource.slang.plugin;

import com.sonarsource.slang.kotlin.InputFileContext;
import com.sonarsource.slang.parser.SLangConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricVisitorTest {

  private NoSonarFilter mockNoSonarFilter;
  private SLangConverter parser = new SLangConverter();
  private MetricVisitor visitor;
  private SensorContextTester sensorContext;
  private DefaultInputFile inputFile;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    sensorContext = SensorContextTester.create(tempFolder.getRoot());
    FileLinesContext mockFileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory mockFileLinesContextFactory = mock(FileLinesContextFactory.class);
    mockNoSonarFilter = mock(NoSonarFilter.class);
    when(mockFileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(mockFileLinesContext);
    visitor = new MetricVisitor(mockFileLinesContextFactory, mockNoSonarFilter);
  }

  @Test
  public void emptySource() throws Exception {
    scan("");
    assertThat(visitor.linesOfCode()).isEmpty();
    assertThat(visitor.commentLines()).isEmpty();
    assertThat(visitor.numberOfFunctions()).isEqualTo(0);
    verify(mockNoSonarFilter).noSonarInFile(inputFile, new HashSet<>());
  }

  @Test
  public void linesOfCode() throws Exception {
    scan("" +
      "x + 1;\n" +
      "// comment\n" +
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.linesOfCode()).containsExactly(1, 3, 4);
  }

  @Test
  public void commentLines() throws Exception {
    scan("" +
      "x + 1;\n" +
      "// comment\n" +
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.commentLines()).containsExactly(2, 3);
  }

  @Test
  public void multiLineComment() throws Exception {
    scan("" +
      "/*start\n" +
      "x + 1\n" +
      "end*/");
    assertThat(visitor.commentLines()).containsExactly(1, 2, 3);
    assertThat(visitor.linesOfCode()).isEmpty();
  }

  @Test
  public void nosonarLines() throws Exception {
    scan("" +
      "x + 1;\n" +
      "// NOSONAR comment\n" +
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.nosonarLines()).containsExactly(2);
    Set<Integer> nosonarLines = new HashSet<>();
    nosonarLines.add(2);
    verify(mockNoSonarFilter).noSonarInFile(inputFile, nosonarLines);
  }

  @Test
  public void functions() throws Exception {
    scan("" +
      "x + 1;\n" +
      "x = true || false;");
    assertThat(visitor.numberOfFunctions()).isEqualTo(0);
    scan("" +
      "x + 1;\n" +
      "fun noBodyFunction();\n" + // Only functions with implementation bodies are considered for the metric
      "fun() { x = 1; }\n" + // Anonymous functions are not considered for function metric computation
      "fun function1() { // comment\n" +
      "x = true || false; }");
    assertThat(visitor.numberOfFunctions()).isEqualTo(1);
  }

  @Test
  public void classes() throws Exception {
    scan("" +
            "x + 1;\n" +
            "x = true || false;");
    assertThat(visitor.numberOfClasses()).isEqualTo(0);
    scan("" +
            "class C(val a: Int, val b: Int) {}\n" +
            "fun function() {}\n" +
            "class D() {}\n" +
            "class E(var a: String) {\n" +
            "  fun doSomething(): String = a\n" +
            "}");
    assertThat(visitor.numberOfFunctions()).isEqualTo(3);
  }

  private void scan(String code) throws IOException {
    inputFile = new TestInputFileBuilder("moduleKey", tempFolder.newFile().getName())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(code).build();
    InputFileContext ctx = new InputFileContext(sensorContext, inputFile);
    visitor.scan(ctx, parser.parse(code));
  }

}
