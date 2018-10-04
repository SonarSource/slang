package org.sonarsource.scala.plugin;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ScoverageSensorTest {


  private static final Path COVERAGE_DIR = Paths.get("src", "test", "resources", "coverage");
  private static final String MODULE_KEY = "/Absolute/Path/To/";

  @Rule
  public LogTester logTester = new LogTester();

  private static ScoverageSensor scoverageSensor = new ScoverageSensor();

  @Test
  public void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    scoverageSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Scoverage sensor for Scala coverage");
  }

  @Test
  public void testFilesCoverage() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("scoverage.xml");

    SensorContextTester context = getSensorContext(reportPath.toString(), "file1.scala", "file2.scala");

    new ScoverageSensor().execute(context);

    String fileKey1 = MODULE_KEY + ":file1.scala";
    String fileKey2 = MODULE_KEY + ":file2.scala";

    //File1
    assertThat(context.lineHits(fileKey1, 6)).isNull();
    assertThat(context.lineHits(fileKey1, 7)).isEqualTo(1);
    assertThat(context.lineHits(fileKey1, 8)).isNull();
    assertThat(context.lineHits(fileKey1, 9)).isNull();

    //File2
    assertThat(context.lineHits(fileKey2, 5)).isEqualTo(1);
    assertThat(context.lineHits(fileKey2, 7)).isEqualTo(1);
    assertThat(context.lineHits(fileKey2, 9)).isNull();
  }

  @Test
  public void testLogWhenInvalidAttribute() throws IOException {
    Path baseDir = COVERAGE_DIR.toAbsolutePath();
    Path reportPath = baseDir.resolve("badscoverage.xml");

    SensorContextTester context = getSensorContext(reportPath.toString(), "file1.scala", "file2.scala");

    new ScoverageSensor().execute(context);

    String expectedMessage = String.format(
        "Some attributes of statement at line 3 of scoverage report are not present.");
    assertThat(logTester.logs().contains(expectedMessage)).isTrue();
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
      // if report is relative path we create it under fake filesystem
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
