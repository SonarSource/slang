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
package org.sonar.go.coverage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonarsource.slang.testing.ThreadLocalLogTester;
import org.sonar.go.coverage.GoCoverSensor.Coverage;
import org.sonar.go.coverage.GoCoverSensor.CoverageStat;
import org.sonar.go.coverage.GoCoverSensor.FileCoverage;
import org.sonar.go.coverage.GoCoverSensor.LineCoverage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoCoverSensorTest {

  static final Path COVERAGE_DIR = Paths.get("src", "test", "resources", "coverage");

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();


  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    GoCoverSensor coverSensor = new GoCoverSensor();
    coverSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Go Cover sensor for Go coverage");
  }

  @Test
  void test_failure() {
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    context.settings().setProperty("sonar.go.coverage.reportPaths", "invalid-coverage-path.out");
    GoCoverSensor coverSensor = new GoCoverSensor();
    coverSensor.execute(context);
    assertThat(logTester.logs(Level.ERROR))
      .containsExactly("Coverage report can't be loaded, report file not found, ignoring this file invalid-coverage-path.out.");
  }

  @Test
  void mode_line() {
    Predicate<String> regexp = (line) -> GoCoverSensor.MODE_LINE_REGEXP.matcher(line).matches();
    assertThat(regexp.test("mode: set")).isTrue();
    assertThat(regexp.test("mode: count")).isTrue();
    assertThat(regexp.test("mode: atomic")).isTrue();
    assertThat(regexp.test("my-app/my-app.go:3.2,3.10 1 1")).isFalse();
  }

  @Test
  void line_regexp() {
    Predicate<String> regexp = (line) -> GoCoverSensor.COVERAGE_LINE_REGEXP.matcher(line).matches();
    assertThat(regexp.test("my-app/my-app.go:3.2,3.10 1 1")).isTrue();
    assertThat(regexp.test("_/my-app/my-app.go:3.2,3.10 1 21")).isTrue();
    assertThat(regexp.test("my-app\\my-app.go:3.2,3.10 1 0")).isTrue();
    assertThat(regexp.test("_\\C_\\my-app\\my-app.go:3.2,3.10 1 42")).isTrue();
    assertThat(regexp.test("mode: set")).isFalse();
  }

  @Test
  void coverage_stat() {
    CoverageStat coverage = new CoverageStat(2, "_/my-app/my-app.go:3.10,4.5 2 234");
    assertThat(coverage.filePath).isEqualTo("_/my-app/my-app.go");
    assertThat(coverage.startLine).isEqualTo(3);
    assertThat(coverage.startCol).isEqualTo(10);
    assertThat(coverage.endLine).isEqualTo(4);
    assertThat(coverage.endCol).isEqualTo(5);
    // numStmt is not parsed because not required.
    assertThat(coverage.count).isEqualTo(234);

    assertThatThrownBy(() -> new CoverageStat(42, "invalid") )
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid go coverage at line 42");
  }

  @Test
  void line_coverage() {
    LineCoverage line = new LineCoverage();
    assertThat(line.hits).isZero();

    line.add(new CoverageStat(2, "main.go:2.2,2.5 1 0"));
    assertThat(line.hits).isZero();

    line.add(new CoverageStat(2, "main.go:2.2,2.5 1 3"));
    assertThat(line.hits).isEqualTo(3);

    line.add(new CoverageStat(2, "main.go:2.2,2.5 1 2"));
    assertThat(line.hits).isEqualTo(5);

    line.add(new CoverageStat(2, "main.go:2.8,2.10 1 0"));
    assertThat(line.hits).isEqualTo(5);
  }

  @Test
  void line_coverage_over_flow() {
    LineCoverage line = new LineCoverage();
    // hits is greater than Integer.MAX_VALUE
    line.add(new CoverageStat(2, "main.go:2.2,2.5 1 " + + (((long)Integer.MAX_VALUE) + 1)));
    assertThat(line.hits).isEqualTo(Integer.MAX_VALUE);

    LineCoverage lineWithTwoStats = new LineCoverage();
    // hits is greater than Integer.MAX_VALUE
    lineWithTwoStats.add(new CoverageStat(2, "main.go:2.2,2.5 1 " + (Integer.MAX_VALUE - 1)));
    lineWithTwoStats.add(new CoverageStat(2, "main.go:2.2,2.5 1 2"));
    assertThat(line.hits).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void line_coverage_do_not_parse_num_statement() {
    LineCoverage line = new LineCoverage();
    line.add(new CoverageStat(2, "main.go:2.2,2.5 2650701153 0"));
    assertThat(line.hits).isZero();
  }

  @Test
  void file_coverage() throws Exception {
    List<CoverageStat> coverageStats = Arrays.asList(
      new CoverageStat(2, "cover.go:4.11,6.3 1 3"),
      new CoverageStat(3, "cover.go:6.3,8.3 1 0"));
    FileCoverage file = new FileCoverage(coverageStats, Files.readAllLines(COVERAGE_DIR.resolve("cover.go")));

    assertThat(file.lineMap.keySet()).containsExactlyInAnyOrder(5, 6, 7);
    assertThat(file.lineMap.get(4)).isNull();
    assertThat(file.lineMap.get(5).hits).isEqualTo(3);
    assertThat(file.lineMap.get(6).hits).isZero();
    assertThat(file.lineMap.get(7).hits).isZero();
    assertThat(file.lineMap.get(8)).isNull();
  }

  @Test
  void file_coverage_empty_lines() throws Exception {
    final String fileName = "cover_empty_lines.go";
    List<CoverageStat> coverageStats = Collections.singletonList(new CoverageStat(2, fileName + ":3.28,9.2 2 1"));
    FileCoverage file = new FileCoverage(coverageStats, Files.readAllLines(COVERAGE_DIR.resolve(fileName)));

    assertThat(file.lineMap.keySet()).containsExactlyInAnyOrder(5, 7);
  }

  @Test
  void coverage() {
    GoPathContext linuxContext = new GoPathContext('/', ":", "/home/paul/go");
    Coverage coverage = new Coverage(linuxContext);
    coverage.add(new CoverageStat(2, "main.go:2.2,2.5 1 1"));
    coverage.add(new CoverageStat(3, "main.go:4.2,4.7 1 0"));
    coverage.add(new CoverageStat(4, "other.go:3.2,4.12 1 1"));
    assertThat(coverage.fileMap.keySet()).containsExactlyInAnyOrder("/home/paul/go/src/main.go", "/home/paul/go/src/other.go");
    List<CoverageStat> coverageStats = coverage.fileMap.get("/home/paul/go/src/main.go");
    FileCoverage fileCoverage = new FileCoverage(coverageStats, null);
    assertThat(fileCoverage.lineMap.keySet()).containsExactlyInAnyOrder(2, 4);
    assertThat(new FileCoverage(coverage.fileMap.get("/home/paul/go/src/other.go"), null).lineMap.keySet()).containsExactlyInAnyOrder(3, 4);
  }

  @Test
  void parse_coverage_linux_relative() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.linux.relative.out");
    GoPathContext linuxContext = new GoPathContext('/', ":", "/home/paul/go");
    String coverPath = "/home/paul/go/src/github.com/SonarSource/slang/sonar-go-plugin/src/test/resources/coverage/cover.go";
    assertCoverGo(coverageFile, linuxContext, coverPath);
  }

  @Test
  void parse_coverage_linux_absolute() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.linux.absolute.out");
    GoPathContext linuxContext = new GoPathContext('/', ":", "/home/paul/go");
    String coverPath = "/home/paul/dev/github/SonarSource/slang/sonar-go-plugin/src/test/resources/coverage/cover.go";
    assertCoverGo(coverageFile, linuxContext, coverPath);
  }

  @Test
  void parse_coverage_windows_relative() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.win.relative.out");
    GoPathContext windowsContext = new GoPathContext('\\', ";", "C:\\Users\\paul\\go");
    String coverPath = "C:\\Users\\paul\\go\\src\\github.com\\SonarSource\\slang\\sonar-go-plugin\\src\\test\\resources\\coverage\\cover.go";
    assertCoverGo(coverageFile, windowsContext, coverPath);
  }

  @Test
  void parse_coverage_windows_absolute() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.win.absolute.out");
    GoPathContext windowsContext = new GoPathContext('\\', ";", "C:\\Users\\paul\\go");
    String coverPath = "C:\\Users\\paul\\dev\\github\\SonarSource\\slang\\sonar-go-plugin\\src\\test\\resources\\coverage\\cover.go";
    assertCoverGo(coverageFile, windowsContext, coverPath);
  }

  @Test
  void parse_coverage_one_broken_line() {
    Path coverageFile = COVERAGE_DIR.resolve("coverage.one.broken.line.out");
    GoPathContext linuxContext = new GoPathContext('/', ":", "/home/paul/go");
    String coverPath = "/home/paul/go/src/github.com/SonarSource/slang/sonar-go-plugin/src/test/resources/coverage/cover.go";
    assertCoverGo(coverageFile, linuxContext, coverPath);

    assertThat(logTester.logs(Level.DEBUG))
      .containsExactly("Ignoring line in coverage report: Invalid go coverage at line 7.");
  }

  @Test
  void get_report_paths() {
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    context.setSettings(new MapSettings());
    Path coverageFile1 = COVERAGE_DIR.resolve("coverage.linux.relative.out").toAbsolutePath();
    context.settings().setProperty("sonar.go.coverage.reportPaths",
      coverageFile1 + ",coverage.linux.absolute.out");
    Stream<Path> reportPaths = GoCoverSensor.getReportPaths(context);
    assertThat(reportPaths).containsExactlyInAnyOrder(
      coverageFile1,
      Paths.get("src", "test", "resources", "coverage", "coverage.linux.absolute.out"));
  }

  @Test
  void get_report_paths_with_wildcards() {
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths",
      "*.absolute.out,glob" + File.separator +"*.out, test*" + File.separator +"*.out, coverage?.out");
    Stream<Path> reportPaths = GoCoverSensor.getReportPaths(context);
    assertThat(reportPaths).containsExactlyInAnyOrder(
      Paths.get("src", "test", "resources", "coverage", "coverage.linux.absolute.out"),
      Paths.get("src", "test", "resources", "coverage", "coverage.win.absolute.out"),
      Paths.get("src", "test", "resources", "coverage", "glob", "coverage.glob.out"),
      Paths.get("src", "test", "resources", "coverage", "test1", "coverage.out"),
      Paths.get("src", "test", "resources", "coverage", "coverage1.out"));

    context.settings().setProperty("sonar.go.coverage.reportPaths",
      "**" + File.separator +"coverage.glob.out");
    Stream<Path> reportPaths2 = GoCoverSensor.getReportPaths(context);
    assertThat(reportPaths2).containsExactlyInAnyOrder(
      Paths.get("src", "test", "resources", "coverage", "glob", "coverage.glob.out"));
  }

  @Test
  void should_continue_if_parsing_fails() {
    SensorContextTester context = SensorContextTester.create(COVERAGE_DIR);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths",
      "test1" + File.separator + "coverage.out, coverage.relative.out");
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    GoPathContext goContext = new GoPathContext(File.separatorChar, File.pathSeparator, baseDir.toString());
    GoCoverSensor sensor = new GoCoverSensor();
    sensor.execute(context, goContext);
    assertThat(logTester.logs(Level.ERROR)).hasSize(1);
    assertThat(logTester.logs(Level.ERROR).get(0)).endsWith("coverage.out: Invalid go coverage, expect 'mode:' on the first line.");
  }

  @Test
  void upload_reports() throws IOException {
    String fileName = "cover.go";
    SensorContextTester context = setUpContext(fileName, "coverage.relative.out");
    String fileKey = "moduleKey:" + fileName;
    assertThat(context.lineHits(fileKey, 3)).isNull();
    assertThat(context.lineHits(fileKey, 4)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 5)).isEqualTo(2);
    assertThat(context.conditions(fileKey, 5)).isNull();
    assertThat(context.coveredConditions(fileKey, 5)).isNull();
    assertThat(context.lineHits(fileKey, 6)).isZero();
    assertThat(context.lineHits(fileKey, 7)).isZero();
    assertThat(context.lineHits(fileKey, 8)).isNull();
  }

  @Test
  void coverage_fuzzy_inputfile() throws Exception {
    String fileName = "cover.go";
    SensorContextTester context = setUpContext(fileName, "coverage.fuzzy.out");
    String fileKey = "moduleKey:" + fileName;
    assertThat(context.lineHits(fileKey, 3)).isNull();
    assertThat(context.lineHits(fileKey, 4)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 5)).isEqualTo(2);
    assertThat(context.conditions(fileKey, 5)).isNull();
    assertThat(context.coveredConditions(fileKey, 5)).isNull();
    assertThat(context.lineHits(fileKey, 6)).isZero();
    assertThat(context.lineHits(fileKey, 7)).isZero();
    assertThat(context.lineHits(fileKey, 8)).isNull();

    String ignoredFileLog = "File 'doesntexists.go' is not included in the project, ignoring coverage";
    assertThat(logTester.logs(Level.WARN)).contains(ignoredFileLog);
  }

  @Test
  void coverage_switch_case() throws Exception {
    String fileName = "coverage.switch.go";
    SensorContextTester context = setUpContext(fileName, "coverage.switch.out");
    String fileKey = "moduleKey:" + fileName;
    // Opening brace of function should not be included into the switch
    assertThat(context.lineHits(fileKey, 3)).isNull();
    assertThat(context.lineHits(fileKey, 4)).isEqualTo(1);
    // Switch case should not be counted
    assertThat(context.lineHits(fileKey, 5)).isNull();
    assertThat(context.lineHits(fileKey, 6)).isEqualTo(1);
    assertThat(context.lineHits(fileKey, 7)).isNull();
    assertThat(context.lineHits(fileKey, 8)).isZero();
    assertThat(context.lineHits(fileKey, 9)).isNull();
    assertThat(context.lineHits(fileKey, 10)).isZero();
    assertThat(context.lineHits(fileKey, 11)).isNull();
    assertThat(context.lineHits(fileKey, 12)).isZero();
    assertThat(context.lineHits(fileKey, 13)).isNull();
    assertThat(context.lineHits(fileKey, 14)).isZero();
  }

  private SensorContextTester setUpContext(String fileName, String coverageFile) throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setSettings(new MapSettings());
    context.settings().setProperty("sonar.go.coverage.reportPaths", coverageFile);
    Path goFilePath = baseDir.resolve(fileName);
    String content = new String(Files.readAllBytes(goFilePath), UTF_8);
    context.fileSystem().add(TestInputFileBuilder.create("moduleKey", baseDir.toFile(), goFilePath.toFile())
      .setLanguage("go")
      .setType(InputFile.Type.MAIN)
      .initMetadata(content)
      .setContents(content)
      .build());
    GoPathContext goContext = new GoPathContext(File.separatorChar, File.pathSeparator, "");
    GoCoverSensor sensor = new GoCoverSensor();
    sensor.execute(context, goContext);
    return context;
  }

  private void assertCoverGo(Path coverageFile, GoPathContext goContext, String absolutePath) {
    Coverage coverage = new Coverage(goContext);
    GoCoverSensor.parse(coverageFile, coverage);
    assertThat(coverage.fileMap.keySet()).containsExactlyInAnyOrder(absolutePath);
    List<CoverageStat> coverageStats = coverage.fileMap.get(absolutePath);
    FileCoverage fileCoverage = new FileCoverage(coverageStats, null);
    assertThat(fileCoverage.lineMap.keySet()).containsExactlyInAnyOrder(3, 4, 5, 6, 7, 8);
    assertThat(fileCoverage.lineMap.get(2)).isNull();
    assertThat(fileCoverage.lineMap.get(3).hits).isEqualTo(1);
    assertThat(fileCoverage.lineMap.get(4).hits).isEqualTo(2);
    assertThat(fileCoverage.lineMap.get(5).hits).isEqualTo(2);
    assertThat(fileCoverage.lineMap.get(6).hits).isZero();
    assertThat(fileCoverage.lineMap.get(7).hits).isZero();
    assertThat(fileCoverage.lineMap.get(8).hits).isZero();
    assertThat(fileCoverage.lineMap.get(9)).isNull();
  }

}
