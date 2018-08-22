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
package org.sonarsource.ruby.plugin;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class SimpleCovReportTest {

  private static final Path COVERAGE_DIR = Paths.get("src", "test", "resources", "coverage");

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test_relative_report_path() throws IOException {
    SensorContextTester context = getSensorContext("resultset.json", "file1.rb");
    SimpleCovReport.saveCoverageReports(context);

    String fileKey = "moduleKey:file1.rb";
    assertThat(context.lineHits(fileKey, 1)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 2)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 3)).isEqualTo(2);
    assertThat(context.lineHits(fileKey, 4)).isNull();
    assertThat(context.lineHits(fileKey, 5)).isNull();
    assertThat(context.lineHits(fileKey, 6)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 7)).isEqualTo(0);
  }

  @Test
  public void test_absolute_report_path() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("resultset.json");
    SensorContextTester context = getSensorContext(reportPath.toString(), "file1.rb");
    SimpleCovReport.saveCoverageReports(context);

    String fileKey = "moduleKey:file1.rb";
    assertThat(context.lineHits(fileKey, 1)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 3)).isEqualTo(2);
    assertThat(context.lineHits(fileKey, 4)).isNull();
    assertThat(context.lineHits(fileKey, 7)).isEqualTo(0);
  }

  @Test
  public void test_merged_resultset() throws IOException {
    SensorContextTester context = getSensorContext("merged_resultset.json", "file1.rb", "file2.rb");
    SimpleCovReport.saveCoverageReports(context);

    String file1Key = "moduleKey:file1.rb";
    assertThat(context.lineHits(file1Key, 1)).isEqualTo(0);
    assertThat(context.lineHits(file1Key, 2)).isEqualTo(0);
    assertThat(context.lineHits(file1Key, 3)).isEqualTo(1);
    assertThat(context.lineHits(file1Key, 4)).isEqualTo(0);
    assertThat(context.lineHits(file1Key, 5)).isNull();
    assertThat(context.lineHits(file1Key, 6)).isEqualTo(1);
    assertThat(context.lineHits(file1Key, 7)).isEqualTo(1);
    assertThat(context.lineHits(file1Key, 8)).isEqualTo(1);
    assertThat(context.lineHits(file1Key, 9)).isEqualTo(2);

    String file2Key = "moduleKey:file2.rb";
    assertThat(context.lineHits(file2Key, 1)).isEqualTo(3);
  }

  @Test
  public void test_multi_resultsets() throws IOException {
    SensorContextTester context = getSensorContext("resultset_1.json, resultset_2.json", "file1.rb", "file2.rb");
    SimpleCovReport.saveCoverageReports(context);

    String file1Key = "moduleKey:file1.rb";
    assertThat(context.lineHits(file1Key, 1)).isEqualTo(0);
    assertThat(context.lineHits(file1Key, 2)).isEqualTo(0);
    assertThat(context.lineHits(file1Key, 3)).isEqualTo(1);
    assertThat(context.lineHits(file1Key, 4)).isEqualTo(0);
    assertThat(context.lineHits(file1Key, 5)).isNull();
    assertThat(context.lineHits(file1Key, 6)).isEqualTo(1);
    assertThat(context.lineHits(file1Key, 7)).isEqualTo(1);
    assertThat(context.lineHits(file1Key, 8)).isEqualTo(1);
    assertThat(context.lineHits(file1Key, 9)).isEqualTo(2);

    String file2Key = "moduleKey:file2.rb";
    assertThat(context.lineHits(file2Key, 1)).isEqualTo(3);
  }

  @Test
  public void no_measure_on_files_not_in_context() throws IOException {
    SensorContextTester context = spy(getSensorContext("additional_file_resultset.json", "file2.rb"));
    SimpleCovReport.saveCoverageReports(context);

    // assert that newCoverage method is called only once on file2
    verify(context, times(1)).newCoverage();
    assertThat(context.lineHits("moduleKey:file2.rb", 1)).isEqualTo(5);
  }

  @Test
  public void log_when_wrong_line_numbers() throws IOException {
    SensorContextTester context = getSensorContext("wrong_lines_resultset.json", "file2.rb");
    SimpleCovReport.saveCoverageReports(context);

    String expectedMessage = "Invalid coverage information on file: '/Absolute/Path/To/file2.rb'";
    assertThat(logTester.logs().contains(expectedMessage)).isTrue();
  }

  @Test
  public void log_when_invalid_format() throws IOException {
    SensorContextTester context = getSensorContext("invalid_resultset.json", "file1.rb");
    SimpleCovReport.saveCoverageReports(context);

    String expectedMessage = String.format(
      "Cannot read coverage report file, expecting standard SimpleCov resultset JSON format: '%s/invalid_resultset.json'",
      COVERAGE_DIR.toAbsolutePath().toString());
    assertThat(logTester.logs().contains(expectedMessage)).isTrue();
  }

  @Test
  public void log_when_invalid_report_path() throws IOException {
    SensorContextTester context = getSensorContext("noFile.json", "file1.rb");
    SimpleCovReport.saveCoverageReports(context);

    assertThat(logTester.logs().contains("SimpleCov report not found: 'noFile.json'")).isTrue();
  }

  @Test
  public void success_for_report_present() throws IOException {
    SensorContextTester context = getSensorContext("noFile2.json,resultset_2.json", "file1.rb", "file2.rb");
    SimpleCovReport.saveCoverageReports(context);

    assertThat(logTester.logs().contains("SimpleCov report not found: 'noFile2.json'")).isTrue();

    assertThat(context.lineHits("moduleKey:file1.rb", 9)).isEqualTo(1);
    assertThat(context.lineHits("moduleKey:file2.rb", 1)).isEqualTo(3);
  }

  private SensorContextTester getSensorContext(String coverageReportPath, String... fileNames) throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.ruby.coverage.reportPaths", coverageReportPath);

    DefaultFileSystem defaultFileSystem = spy(new DefaultFileSystem(baseDir)).setEncoding(Charset.defaultCharset());
    Map<String, InputFile> inputFilesMap = new HashMap<>();

    for (String fileName : fileNames) {
      DefaultInputFile inputFile = getDefaultInputFile(baseDir, fileName);
      defaultFileSystem.add(inputFile);
      inputFilesMap.put("/Absolute/Path/To/" + fileName, inputFile);
    }

    doAnswer(invocationOnMock ->
      inputFilesMap.entrySet().stream()
        .filter(entry -> new FilePredicateMatcher(entry.getKey()).matches(invocationOnMock.getArgument(0)))
        .findFirst()
        .map(Map.Entry::getValue)
        .orElse(null))
      .when(defaultFileSystem)
      .inputFile(any());

    context.setFileSystem(defaultFileSystem);
    return context;
  }


  private DefaultInputFile getDefaultInputFile(Path baseDir, String fileName) throws IOException {
    Path filePath = baseDir.resolve(fileName);
    String content = new String(Files.readAllBytes(filePath), UTF_8);
    return TestInputFileBuilder.create("moduleKey", baseDir.toFile(), filePath.toFile())
      .setLanguage("ruby")
      .setType(InputFile.Type.MAIN)
      .initMetadata(content)
      .setContents(content)
      .build();
  }

  class FilePredicateMatcher implements ArgumentMatcher<FilePredicate> {
    private String absolutePath;

    FilePredicateMatcher(String absolutePath) {
      this.absolutePath = absolutePath;
    }

    public boolean matches(FilePredicate filePredicate) {
      InputFile mockInputFile = mock(InputFile.class);
      when(mockInputFile.absolutePath()).thenReturn(absolutePath);
      return filePredicate.apply(mockInputFile);
    }
  }

}