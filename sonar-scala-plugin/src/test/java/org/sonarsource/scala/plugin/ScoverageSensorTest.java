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
package org.sonarsource.scala.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.ThreadLocalLogTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ScoverageSensorTest {

  private static final Path COVERAGE_DIR = Paths.get("src", "test", "resources", "coverage");
  private static final String MODULE_KEY = "/Absolute/Path/To/";
  private static final List<String> ANALYSIS_WARNINGS = new ArrayList<>();

  @Before
  public void setup() {
    ANALYSIS_WARNINGS.clear();
  }

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    newSCoverageSensor().describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Scoverage sensor for Scala coverage");
  }

  @Test
  public void test_missing_file() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("missing-file.xml");

    SensorContextTester context = getSensorContext(reportPath.toString());

    newSCoverageSensor().execute(context);

    List<String> warnings = logTester.logs(LoggerLevel.WARN);
    assertThat(warnings)
      .hasSize(1)
      .hasSameSizeAs(ANALYSIS_WARNINGS);
    assertThat(warnings.get(0))
      .startsWith("Unable to import Scoverage report file(s):")
      .contains("missing-file.xml")
      .endsWith("The report file(s) can not be found. Check that the property 'sonar.scala.coverage.reportPaths' is correctly configured.");
    assertThat(ANALYSIS_WARNINGS.get(0))
      .startsWith("Unable to import 1 Scoverage report file(s).")
      .endsWith("Please check that property 'sonar.scala.coverage.reportPaths' is correctly configured and the analysis logs for more details.");
  }

  @Test
  public void testFilesCoverage() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("scoverage.xml");

    SensorContextTester context = getSensorContext(reportPath.toString(), "file1.scala", "file2.scala");

    newSCoverageSensor().execute(context);

    String fileKey1 = MODULE_KEY + ":file1.scala";
    String fileKey2 = MODULE_KEY + ":file2.scala";

    //File1
    assertThat(context.lineHits(fileKey1, 5)).isNull();
    assertThat(context.lineHits(fileKey1, 6)).isZero();
    assertThat(context.lineHits(fileKey1, 7)).isEqualTo(1);
    assertThat(context.lineHits(fileKey1, 8)).isZero();
    assertThat(context.lineHits(fileKey1, 9)).isNull();

    //File2
    assertThat(context.lineHits(fileKey2, 5)).isEqualTo(1);
    assertThat(context.lineHits(fileKey2, 6)).isNull();
    assertThat(context.lineHits(fileKey2, 7)).isEqualTo(2);
    assertThat(context.lineHits(fileKey2, 8)).isNull();
    assertThat(context.lineHits(fileKey2, 9)).isZero();
  }

  @Test
  public void testLogWhenInvalidAttribute() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("badscoverage.xml");

    SensorContextTester context = getSensorContext(reportPath.toString(), "file1.scala", "file2.scala");

    newSCoverageSensor().execute(context);

    String expectedMessage = "File '" + reportPath.toString() + "' can't be read. java.lang.NumberFormatException: null";
    assertThat(logTester.logs().contains(expectedMessage)).isTrue();
  }

  @Test
  public void testLogWhenUnresolvedInputFiles() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("scoverage.xml");

    SensorContextTester context = getSensorContext(reportPath.toString());

    newSCoverageSensor().execute(context);

    String expectedMessage = "Fail to resolve 2 file(s). No coverage data will be imported on the following file(s): /Absolute/Path/To/file1.scala;/Absolute/Path/To/file2.scala";

    assertThat(logTester.logs().contains(expectedMessage)).isTrue();
  }

  @Test
  public void testLogInvalidXMLFile() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("invalidscoverage.xml");

    SensorContextTester context = getSensorContext(reportPath.toString());

    newSCoverageSensor().execute(context);

    String expectedMessage = String.format("File '" + reportPath.toString() +
        "' can't be read. com.ctc.wstx.exc.WstxUnexpectedCharException: Unexpected character 's' (code 115) in prolog; expected '<'");
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith(expectedMessage);
  }

  @Test
  public void testStringInLineNumber() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("stringInLine.xml");

    SensorContextTester context = getSensorContext(reportPath.toString());

    newSCoverageSensor().execute(context);

    String expectedMessage = "File '" + reportPath.toString() + "' can't be read. java.lang.NumberFormatException";
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).contains(expectedMessage);
  }

  private static ScoverageSensor newSCoverageSensor() {
    return new ScoverageSensor(ANALYSIS_WARNINGS::add);
  }

  private SensorContextTester getSensorContext(String coverageReportPath, String... fileNames) throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.scala.coverage.reportPaths", coverageReportPath);

    DefaultFileSystem defaultFileSystem = new DefaultFileSystem(new File(MODULE_KEY));
    createReportFiles(coverageReportPath, baseDir, defaultFileSystem);
    for (String fileName : fileNames) {
      DefaultInputFile inputFile = createInputFile(fileName, fileContent(baseDir, fileName));
      defaultFileSystem.add(inputFile);
    }

    context.setFileSystem(defaultFileSystem);

    return context;
  }

  private void createReportFiles(String coverageReportPath, Path baseDir, DefaultFileSystem defaultFileSystem) throws IOException {
    String[] coverageReportPaths = coverageReportPath.split(",");
    for (String reportPath : coverageReportPaths) {
      reportPath = reportPath.trim();
      if (!Paths.get(coverageReportPath).isAbsolute()) {
        try {
          DefaultInputFile coverageFile = createInputFile(reportPath, fileContent(baseDir, reportPath));
          defaultFileSystem.add(coverageFile);
        } catch (NoSuchFileException e) {
          // tests can simulate non-existing file, this is OK
        }
      }
    }
  }

  private DefaultInputFile createInputFile(String fileName, String content) {
    return TestInputFileBuilder.create(MODULE_KEY, fileName)
        .setType(InputFile.Type.MAIN)
        .initMetadata(content)
        .setContents(content)
        .build();
  }

  private String fileContent(Path baseDir, String fileName) throws IOException {
    Path filePath = baseDir.resolve(fileName);
    return new String(Files.readAllBytes(filePath), UTF_8);
  }
}
