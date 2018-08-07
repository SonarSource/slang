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
package org.sonarsource.slang.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Language;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.CommentedCodeCheck;
import org.sonarsource.slang.checks.IdenticalBinaryOperandCheck;
import org.sonarsource.slang.checks.StringLiteralDuplicatedCheck;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.parser.SLangConverter;
import org.sonarsource.slang.parser.SlangCodeVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class SlangSensorTest {

  private static final String REPOSITORY_KEY = "slang";

  @org.junit.Rule
  public JUnitTempFolder temp = new JUnitTempFolder();

  private File baseDir;
  private SensorContextTester context;
  private FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void setup() {
    baseDir = temp.newDir();
    context = SensorContextTester.create(baseDir);
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
  }

  @Test
  public void test_one_rule() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "fun main() {\nprint (1 == 1);}");
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
    InputFile inputFile = createInputFile("file1.slang", "" +
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
  public void test_commented_code() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "fun main() {\n" +
      "// fun foo() { if (true) {print(\"string literal\");}}\n" +
      "print (1 == 1);\n" +
      "print(b);\n" +
      "// a b c ...\n" +
      "foo();\n" +
      "// Coefficients of polynomial\n" +
      "val b = DoubleArray(n); // linear\n" +
      "val c = DoubleArray(n + 1); // quadratic\n" +
      "val d = DoubleArray(n); // cubic\n" +
      "}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S125");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S125");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Remove this commented out code.");
  }

  @Test
  public void simple_file() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "fun main(int x) {\nprint (1 == 1); print(\"abc\"); }\nclass A {}");
    context.fileSystem().add(inputFile);
    sensor(checkFactory()).execute(context);
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).containsExactly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 3)).isEmpty();
    assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(3);
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isEqualTo(0);
    assertThat(context.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isEqualTo(1);
    assertThat(context.cpdTokens(inputFile.key()).get(1).getValue()).isEqualTo("print(1==1);print(LITERAL);}");
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMPLEXITY).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(2);

    assertThat(logTester.logs()).contains("1 source files to be analyzed");
  }

  @Test
  public void test_fail_input() throws IOException {
    InputFile inputFile = createInputFile("fakeFile.slang", "");
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(spyInputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(spyInputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: fakeFile.slang");
    assertThat(analysisError.location()).isNull();

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. ", inputFile.uri()));
  }

  @Test
  public void test_fail_parsing() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "\n class A {\n" +
      " fun x() {}\n" +
      " fun y() {}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.slang");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isNotNull();
    assertThat(textPointer.line()).isEqualTo(2);
    assertThat(textPointer.lineOffset()).isEqualTo(1);

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. Parse error at position 2:1", inputFile.uri()));
  }

  @Test
  public void test_failure_in_check() {
    InputFile inputFile = createInputFile("file1.slang", "fun f() {}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = mock(CheckFactory.class);
    Checks checks = mock(Checks.class);
    SlangCheck failingCheck = init ->
      init.register(TopLevelTree.class, (ctx, tree) -> {
        throw new IllegalStateException("BOUM");
      });
    when(checks.ruleKey(failingCheck)).thenReturn(RuleKey.of("slang", "failing"));
    when(checkFactory.create(REPOSITORY_KEY)).thenReturn(checks);
    when(checks.all()).thenReturn(Collections.singletonList(failingCheck));
    sensor(checkFactory).execute(context);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(logTester.logs()).contains("Cannot analyse file1.slang");
  }

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    SlangSensor sensor = sensor(mock(CheckFactory.class));
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.languages()).hasSize(1);
    assertThat(sensorDescriptor.languages()).containsExactly("slang");
    assertThat(sensorDescriptor.name()).isEqualTo("SLang Sensor");
  }

  private void assertTextRange(TextRange textRange, int startLine, int startLineOffset, int endLine, int endLineOffset) {
    assertThat(textRange.start().line()).isEqualTo(startLine);
    assertThat(textRange.start().lineOffset()).isEqualTo(startLineOffset);
    assertThat(textRange.end().line()).isEqualTo(endLine);
    assertThat(textRange.end().lineOffset()).isEqualTo(endLineOffset);
  }

  private InputFile createInputFile(String relativePath, String content) {
    return new TestInputFileBuilder("moduleKey", relativePath)
      .setModuleBaseDir(baseDir.toPath())
      .setType(InputFile.Type.MAIN)
      .setLanguage(SlangLanguage.SLANG.getKey())
      .setCharset(StandardCharsets.UTF_8)
      .setContents(content)
      .build();
  }

  private CheckFactory checkFactory(String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String ruleKey : ruleKeys) {
      builder.create(RuleKey.of(REPOSITORY_KEY, ruleKey))
        .setName(ruleKey)
        .activate();
    }
    context.setActiveRules(builder.build());
    return new CheckFactory(context.activeRules());
  }

  private SlangSensor sensor(CheckFactory checkFactory) {
    return new SlangSensor(new NoSonarFilter(), fileLinesContextFactory, SlangLanguage.SLANG) {
      @Override
      protected ASTConverter astConverter() {
        return new SLangConverter();
      }

      @Override
      protected Checks<SlangCheck> checks() {
        Checks<SlangCheck> checks = checkFactory.create(REPOSITORY_KEY);
        checks.addAnnotatedChecks(
          StringLiteralDuplicatedCheck.class,
          new CommentedCodeCheck(new SlangCodeVerifier()),
          IdenticalBinaryOperandCheck.class);
        return checks;
      }
    };
  }

  enum SlangLanguage implements Language {
    SLANG;

    @Override
    public String getKey() {
      return "slang";
    }

    @Override
    public String getName() {
      return "SLang";
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[]{".slang"};
    }
  }


}
