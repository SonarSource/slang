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
package org.sonarsource.kotlin.externalreport.androidlint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.stream.XMLStreamException;
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

public class AndroidLintSensor implements Sensor {

  private static final Logger LOG = Loggers.get(AndroidLintSensor.class);

  static final String LINTER_KEY = "android-lint";

  static final String LINTER_NAME = "Android Lint";

  public static final String REPORT_PROPERTY_KEY = "sonar.androidLint.reportPaths";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .name("Import of Android Lint issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    reportFiles.forEach(report -> importReport(report, context));
  }

  private static void importReport(File reportPath, SensorContext context) {
    try (InputStream in = new FileInputStream(reportPath)) {
      LOG.info("Importing {}", reportPath);
      AndroidLintXmlReportReader.read(in, (id, file, line, message) -> saveIssue(context, id, file, line, message));
    } catch (IOException | XMLStreamException | RuntimeException e) {
      LOG.error("No issues information will be saved as the report file '{}' can't be read.", reportPath, e);
    }
  }

  private static void saveIssue(SensorContext context, String id, String file, String line, String message) {
    if (id.isEmpty() || message.isEmpty() || file.isEmpty() || !AndroidLintRulesDefinition.isTextFile(file)) {
      LOG.debug("Missing information or unsupported file type for id:'{}', file:'{}', message:'{}'", id, file, message);
      return;
    }
    FilePredicates predicates = context.fileSystem().predicates();
    InputFile inputFile = context.fileSystem().inputFile(predicates.or(
      predicates.hasAbsolutePath(file),
      predicates.hasRelativePath(file)));

    if (inputFile == null) {
      LOG.warn("No input file found for {}. No android lint issues will be imported on this file.", file);
      return;
    }
    RuleKey ruleKey = RuleKey.of(LINTER_KEY, id);
    NewExternalIssue newExternalIssue = context.newExternalIssue();
    String ruleKey1 = ruleKey.rule();

    ExternalRuleLoader externalRuleLoader = AndroidLintRulesDefinition.RULE_LOADER;
    newExternalIssue
      .type(externalRuleLoader.ruleType(ruleKey1))
      .severity(externalRuleLoader.ruleSeverity(ruleKey1))
      .remediationEffortMinutes(externalRuleLoader.ruleConstantDebtMinutes(ruleKey1));

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(message)
      .on(inputFile);

    if (!line.isEmpty()) {
      primaryLocation.at(inputFile.selectLine(Integer.parseInt(line)));
    }

    newExternalIssue
      .at(primaryLocation)
      .engineId(ruleKey.repository())
      .ruleId(ruleKey.rule())
      .save();
  }

}
