/*
 * SonarSource SLang
 * Copyright (C) 2018-2019 SonarSource SA
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
package org.sonarsource.ruby.externalreport.rubocop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;
import org.sonarsource.analyzer.commons.internal.json.simple.parser.ParseException;

public class RuboCopSensor implements Sensor {

  private static final Logger LOG = Loggers.get(RuboCopSensor.class);

  static final String LINTER_KEY = "rubocop";

  static final String LINTER_NAME = "RuboCop";

  public static final String REPORT_PROPERTY_KEY = "sonar.ruby.rubocop.reportPaths";

  private static final int MAX_LOGGED_FILE_NAMES = 20;

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .name("Import of RuboCop issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    Set<String> unresolvedInputFile = new HashSet<>();
    reportFiles.forEach(report -> importReport(report, context, unresolvedInputFile));
    logUnresolvedInputFiles(unresolvedInputFile);
  }

  private static void logUnresolvedInputFiles(Set<String> unresolvedInputFile) {
    if (unresolvedInputFile.isEmpty()) {
      return;
    }
    String fileList = unresolvedInputFile.stream().sorted().limit(MAX_LOGGED_FILE_NAMES).collect(Collectors.joining(";"));
    if (unresolvedInputFile.size() > MAX_LOGGED_FILE_NAMES) {
      fileList += ";...";
    }
    LOG.warn("Fail to resolve {} file(s). No RuboCop issues will be imported on the following file(s): {}", unresolvedInputFile.size(), fileList);
  }

  private static void importReport(File reportPath, SensorContext context, Set<String> unresolvedInputFile) {
    try (InputStream in = new FileInputStream(reportPath)) {
      LOG.info("Importing {}", reportPath);
      RuboCopJsonReportReader.read(in, issue -> saveIssue(context, issue, unresolvedInputFile));
    } catch (IOException | RuntimeException | ParseException e) {
      LOG.error("No issues information will be saved as the report file '{}' can't be read. " + e.getMessage(), reportPath, e);
    }
  }

  private static void saveIssue(SensorContext context, RuboCopJsonReportReader.Issue issue, Set<String> unresolvedInputFile) {
    if (isEmpty(issue.ruleKey) || isEmpty(issue.filePath) || isEmpty(issue.message)) {
      LOG.debug("Missing information or unsupported file type for ruleKey:'{}', filePath:'{}', message:'{}'",
        issue.ruleKey, issue.filePath, issue.message);
      return;
    }
    FilePredicates predicates = context.fileSystem().predicates();
    InputFile inputFile = context.fileSystem().inputFile(predicates.hasPath(issue.filePath));
    if (inputFile == null) {
      unresolvedInputFile.add(issue.filePath);
      return;
    }
    RuleKey qualifiedRuleKey = RuleKey.of(LINTER_KEY, issue.ruleKey);
    NewExternalIssue newExternalIssue = context.newExternalIssue();
    String ruleKey = qualifiedRuleKey.rule();

    ExternalRuleLoader externalRuleLoader = RuboCopRulesDefinition.RULE_LOADER;
    newExternalIssue
      .type(externalRuleLoader.ruleType(ruleKey))
      .severity(externalRuleLoader.ruleSeverity(ruleKey))
      .remediationEffortMinutes(externalRuleLoader.ruleConstantDebtMinutes(ruleKey));

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(issue.message)
      .on(inputFile);

    if (issue.startLine !=null) {
      boolean rangeIsProvided = issue.startColumn != null && issue.lastLine != null && issue.lastColumn != null;
      boolean rangeIsValid = rangeIsProvided && (issue.startLine < issue.lastLine || (issue.startLine == issue.lastLine && issue.startColumn <= issue.lastColumn));
      if (rangeIsValid) {
        primaryLocation.at(inputFile.newRange(issue.startLine, issue.startColumn - 1, issue.lastLine, issue.lastColumn));
      } else {
        primaryLocation.at(inputFile.selectLine(issue.startLine));
      }
    }

    newExternalIssue
      .at(primaryLocation)
      .forRule(qualifiedRuleKey)
      .save();
  }

  private static boolean isEmpty(@Nullable String value) {
    return value == null || value.trim().isEmpty();
  }

}
