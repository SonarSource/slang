/*
 * SonarQube Go Plugin
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
package org.sonar.go.externalreport;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.NewRepository;
import org.sonar.api.server.rule.RulesDefinition.NewRule;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.go.plugin.GoLanguage;
import org.sonarsource.slang.plugin.AbstractPropertyHandlerSensor;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractReportSensor extends AbstractPropertyHandlerSensor {

  private static final Logger LOG = Loggers.get(AbstractReportSensor.class);

  static final long DEFAULT_REMEDIATION_COST = 5L;
  static final Severity DEFAULT_SEVERITY = Severity.MAJOR;
  static final String GENERIC_ISSUE_KEY = "issue";

  protected AbstractReportSensor(AnalysisWarnings analysisWarnings, String propertyKey, String propertyName, String configurationkey) {
    super(analysisWarnings, propertyKey, propertyName, configurationkey, GoLanguage.KEY);
  }

  @Nullable
  abstract ExternalIssue parse(String line);

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return file -> importReport(context, file);
  }

  protected String logPrefix() {
    return this.getClass().getSimpleName() + ": ";
  }

  private void importReport(SensorContext context, File report) {
    try {
      for (String line : Files.readAllLines(report.toPath(), UTF_8)) {
        if (!line.isEmpty()) {
          ExternalIssue issue = parse(line);
          if (issue != null) {
            addLineIssue(context, issue);
          }
        }
      }
    } catch (IOException e) {
      LOG.error(logPrefix() + "No issues information will be saved as the report file '{}' can't be read.",
        report.getPath(), e);
    }
  }

  /**
    * Returns a java.io.File for the given path.
    * If path is not absolute, returns a File with module base directory as parent path.
    */
  static File getIOFile(File baseDir, String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      file = new File(baseDir, path);
    }
    return file;
  }

  @CheckForNull
  InputFile getInputFile(SensorContext context, String filePath) {
    FilePredicates predicates = context.fileSystem().predicates();
    InputFile inputFile = context.fileSystem().inputFile(predicates.or(predicates.hasRelativePath(filePath), predicates.hasAbsolutePath(filePath)));
    if (inputFile == null) {
      LOG.warn(logPrefix() + "No input file found for {}. No {} issues will be imported on this file.", filePath, propertyName());
    }
    return inputFile;
  }

  void addLineIssue(SensorContext context, ExternalIssue issue) {
    InputFile inputFile = getInputFile(context, issue.filename);
    if (inputFile != null) {
      NewExternalIssue newExternalIssue = context.newExternalIssue();
      NewIssueLocation primaryLocation = newExternalIssue.newLocation()
        .message(issue.message)
        .on(inputFile)
        .at(inputFile.selectLine(issue.lineNumber));

      newExternalIssue
        .at(primaryLocation)
        .ruleId(issue.ruleKey)
        .engineId(issue.linter)
        .type(issue.type)
        .severity(DEFAULT_SEVERITY)
        .remediationEffortMinutes(DEFAULT_REMEDIATION_COST)
        .save();
    }
  }

  public static void createExternalRuleRepository(Context context, String linterId, String linterName) {
    NewRepository externalRepo = context.createExternalRepository(linterId, GoLanguage.KEY).setName(linterName);
    String pathToRulesMeta = "org/sonar/l10n/go/rules/" + linterId + "/rules.json";

    try (InputStreamReader inputStreamReader = new InputStreamReader(AbstractReportSensor.class.getClassLoader().getResourceAsStream(pathToRulesMeta), StandardCharsets.UTF_8)) {
      JsonArray jsonArray = Json.parse(inputStreamReader).asArray();
      for(JsonValue jsonValue : jsonArray) {
        JsonObject rule = jsonValue.asObject();
        NewRule newRule = externalRepo.createRule(rule.getString("key", null))
          .setName(rule.getString("name", null))
          .setHtmlDescription(rule.getString("description", null));
        newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().constantPerIssue(DEFAULT_REMEDIATION_COST + "min"));
        if (linterId.equals(GoVetReportSensor.LINTER_ID)) {
          newRule.setType(RuleType.BUG);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Can't read resource: " + pathToRulesMeta, e);
    }

    externalRepo.done();
  }

}
