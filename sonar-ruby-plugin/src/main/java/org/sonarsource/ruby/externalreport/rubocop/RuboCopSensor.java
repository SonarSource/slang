/*
 * SonarSource SLang
 * Copyright (C) 2018-2022 SonarSource SA
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;
import org.sonarsource.analyzer.commons.internal.json.simple.parser.ParseException;
import org.sonarsource.ruby.plugin.RubyPlugin;
import org.sonarsource.slang.plugin.AbstractPropertyHandlerSensor;

public class RuboCopSensor extends AbstractPropertyHandlerSensor {

  private static final Logger LOG = Loggers.get(RuboCopSensor.class);

  static final String LINTER_KEY = "rubocop";

  static final String LINTER_NAME = "RuboCop";

  public static final String REPORT_PROPERTY_KEY = "sonar.ruby.rubocop.reportPaths";
  private final Set<String> unresolvedInputFile = new HashSet<>();

  private static final int MAX_LOGGED_FILE_NAMES = 20;

  public RuboCopSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, LINTER_KEY, LINTER_NAME, REPORT_PROPERTY_KEY, RubyPlugin.RUBY_LANGUAGE_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    unresolvedInputFile.clear();
    super.execute(context);
    logUnresolvedInputFiles();
  }

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return file -> importReport(file, context, unresolvedInputFile);
  }

  private void logUnresolvedInputFiles() {
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
    NewExternalIssue newExternalIssue = context.newExternalIssue();

    ExternalRuleLoader externalRuleLoader = RuboCopRulesDefinition.RULE_LOADER;
    newExternalIssue
      .type(externalRuleLoader.ruleType(issue.ruleKey))
      .severity(externalRuleLoader.ruleSeverity(issue.ruleKey))
      .remediationEffortMinutes(externalRuleLoader.ruleConstantDebtMinutes(issue.ruleKey));

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(issue.message)
      .on(inputFile);

    if (issue.startLine !=null) {
      boolean rangeIsProvided = issue.startColumn != null && issue.lastLine != null && issue.lastColumn != null;
      boolean rangeIsValid = rangeIsProvided && (issue.startLine < issue.lastLine || (issue.startLine.equals(issue.lastLine) && issue.startColumn <= issue.lastColumn));
      if (rangeIsValid) {
        primaryLocation.at(inputFile.newRange(issue.startLine, issue.startColumn - 1, issue.lastLine, issue.lastColumn));
      } else {
        primaryLocation.at(inputFile.selectLine(issue.startLine));
      }
    }

    newExternalIssue
      .at(primaryLocation)
      .engineId(LINTER_KEY)
      .ruleId(issue.ruleKey)
      .save();
  }

  private static boolean isEmpty(@Nullable String value) {
    return value == null || value.trim().isEmpty();
  }

}
