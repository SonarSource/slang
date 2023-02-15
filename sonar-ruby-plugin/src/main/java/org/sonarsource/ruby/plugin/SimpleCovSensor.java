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
package org.sonarsource.ruby.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.internal.json.simple.JSONArray;
import org.sonarsource.analyzer.commons.internal.json.simple.JSONObject;
import org.sonarsource.analyzer.commons.internal.json.simple.parser.JSONParser;

public class SimpleCovSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SimpleCovSensor.class);
  private final RubyExclusionsFileFilter rubyExclusionsFileFilter;

  public SimpleCovSensor(RubyExclusionsFileFilter rubyExclusionsFileFilter) {
    this.rubyExclusionsFileFilter = rubyExclusionsFileFilter;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("SimpleCov Sensor for Ruby coverage")
      .onlyOnLanguage(RubyPlugin.RUBY_LANGUAGE_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    try {
      Map<Path, String> reports = getReportFilesAndContents(context);
      if (reports.isEmpty()) {
        return;
      }
      
      JSONParser parser = new JSONParser();
      Map<String, Map<Integer, Integer>> mergedCoverages = new HashMap<>();
      reports.forEach((path, report) -> safeReadCoverageReport(parser, mergedCoverages, path, report));

      saveCoverage(context, mergedCoverages);
    } catch (IOException e) {
      LOG.error("Error reading coverage reports", e);
    }
  }

  private static void safeReadCoverageReport(JSONParser parser, Map<String, Map<Integer, Integer>> mergedCoverages, Path reportPath, String report) {
    try {
      JSONObject parseResult = (JSONObject) parser.parse(report);
      mergeFileCoverages(mergedCoverages, parseResult);
    } catch (Exception e) {
      LOG.error("Cannot read coverage report file, expecting standard SimpleCov JSON formatter output: '{}'", reportPath, e);
    }
  }

  private void saveCoverage(SensorContext context, Map<String, Map<Integer, Integer>> mergedCoverages) {
    FileSystem fileSystem = context.fileSystem();
    FilePredicates predicates = fileSystem.predicates();

    for (Entry<String, Map<Integer, Integer>> coverageForFile : mergedCoverages.entrySet()) {
      String filePath = coverageForFile.getKey();
      InputFile inputFile = fileSystem.inputFile(predicates.hasAbsolutePath(filePath));
      if (inputFile != null) {
        try {
          saveNewCoverage(context, coverageForFile.getValue(), inputFile);
        } catch (IllegalStateException e) {
          LOG.error("Invalid coverage information on file: '{}'", filePath, e);
        }
      } else if (rubyExclusionsFileFilter.isNotExcluded(filePath)){
        LOG.warn("File '{}' is present in coverage report but cannot be found in filesystem", filePath);
      }
    }
  }

  private static void saveNewCoverage(SensorContext context, Map<Integer, Integer> hitsPerLines, InputFile inputFile) {
    NewCoverage newCoverage = context.newCoverage().onFile(inputFile);
    for (Entry<Integer, Integer> hitsPerLine : hitsPerLines.entrySet()) {
      newCoverage.lineHits(hitsPerLine.getKey(), hitsPerLine.getValue());
    }
    newCoverage.save();
  }

  private static void mergeFileCoverages(Map<String, Map<Integer, Integer>> coveragePerFiles, Map<String, JSONObject> upperJsonObjects) {
    upperJsonObjects.forEach((key, value) -> {
      if ("coverage".equals(key)) {
        mergeFrameworkCoveragesFromJsonFormatter(coveragePerFiles, value);
      }
      JSONObject testFrameworkCoverage = (JSONObject) value.get("coverage");
      if (testFrameworkCoverage != null) {
        LOG.warn("Importing SimpleCov resultset JSON will not be supported from simplecov 18.0. Consider using the JSON formatter, available from SimpleCov 20.0");
        mergeFrameworkCoveragesFromResultSet(coveragePerFiles, testFrameworkCoverage);
      }
    });
  }

  private static void mergeFrameworkCoveragesFromJsonFormatter(Map<String, Map<Integer, Integer>> coveragePerFiles, Map<String, JSONObject> fileCoverageObject) {
    fileCoverageObject.forEach((key, value) -> {
      JSONArray hitsPerLine = (JSONArray) value.get("lines");
      mergeHitPerLines(coveragePerFiles, key, hitsPerLine);
    });
  }

  private static void mergeFrameworkCoveragesFromResultSet(Map<String, Map<Integer, Integer>> coveragePerFiles, Map<String, JSONArray> testFrameworkCoveragePerFiles) {
    testFrameworkCoveragePerFiles.forEach((key, value) -> mergeHitPerLines(coveragePerFiles, key, value));
  }

  private static void mergeHitPerLines(Map<String, Map<Integer, Integer>> coveragePerFiles, String currentFile, JSONArray hitsPerLine) {
    Map<Integer, Integer> fileCoverage = coveragePerFiles.computeIfAbsent(currentFile, key -> new HashMap<>());
    for (int i = 0; i < hitsPerLine.size(); i++) {
      Object hits = hitsPerLine.get(i);
      // Hits can be a Long (coverage data available), null or "ignored".
      if (hits instanceof Long) {
        int line = i + 1;
        Integer currentHits = fileCoverage.getOrDefault(line, 0);
        fileCoverage.put(line, currentHits + ((Long) hits).intValue());
      }
    }
  }

  private static Map<Path, String> getReportFilesAndContents(SensorContext context) throws IOException {
    Map<Path, String> reports = new HashMap<>();
    Configuration config = context.config();
    FileSystem fs = context.fileSystem();
    for (String reportPath : config.getStringArray(RubyPlugin.REPORT_PATHS_KEY)) {
      String trimmedPath = reportPath.trim();
      String report = fileContent(fs, trimmedPath);
      if (report != null) {
        reports.put(Paths.get(trimmedPath), report);
      } else if (config.hasKey(RubyPlugin.REPORT_PATHS_KEY)) {
        LOG.error("SimpleCov report not found: '{}'", trimmedPath);
      }
    }
    return reports;
  }

  @CheckForNull
  private static String fileContent(FileSystem fs, String reportPath) throws IOException {
    InputFile report = fs.inputFile(fs.predicates().hasPath(reportPath));
    if (report != null && report.isFile()) {
      return report.contents();
    }
    File reportFile = fs.resolvePath(reportPath);
    if (reportFile.isFile()) {
      return new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8);
    }
    return null;
  }

}
