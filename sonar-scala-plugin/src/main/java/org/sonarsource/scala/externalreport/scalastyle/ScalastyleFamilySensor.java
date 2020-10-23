/*
 * SonarSource SLang
 * Copyright (C) 2018-2020 SonarSource SA
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
package org.sonarsource.scala.externalreport.scalastyle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;
import org.sonarsource.scala.plugin.ScalaPlugin;
import org.sonarsource.slang.plugin.AbstractPropertyHandlerSensor;

public abstract class ScalastyleFamilySensor extends AbstractPropertyHandlerSensor {

  private static final Logger LOG = Loggers.get(ScalastyleFamilySensor.class);

  private static final int MAX_LOGGED_FILE_NAMES = 20;

  private final Set<String> unresolvedInputFiles = new HashSet<>();

  public abstract ExternalRuleLoader ruleLoader();

  protected ScalastyleFamilySensor(AnalysisWarnings analysisWarnings, String propertyKey, String propertyName, String configurationKey) {
    super(analysisWarnings, propertyKey, propertyName, configurationKey, ScalaPlugin.SCALA_LANGUAGE_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    unresolvedInputFiles.clear();
    super.execute(context);
    logUnresolvedInputFiles();
  }

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return report -> importReport(report, context, unresolvedInputFiles);
  }

  private void logUnresolvedInputFiles() {
    if (unresolvedInputFiles.isEmpty()) {
      return;
    }
    String fileList = unresolvedInputFiles.stream().sorted().limit(MAX_LOGGED_FILE_NAMES).collect(Collectors.joining(";"));
    if (unresolvedInputFiles.size() > MAX_LOGGED_FILE_NAMES) {
      fileList += ";...";
    }
    LOG.warn("Fail to resolve {} file path(s) in " + propertyName() + " report. No issues imported related to file(s): {}", unresolvedInputFiles.size(), fileList);
  }

  private void importReport(File reportPath, SensorContext context, Set<String> unresolvedInputFiles) {
    try (InputStream in = new FileInputStream(reportPath)) {
      ScalastyleXmlReportReader.read(in, (file, line, source, message) -> saveIssue(context, file, line, source, message, unresolvedInputFiles));
    } catch (IOException | XMLStreamException | RuntimeException e) {
      LOG.error("No issues information will be saved as the report file '{}' can't be read. " +
        e.getClass().getSimpleName() + ": " + e.getMessage(), reportPath, e);
    }
  }

  private void saveIssue(SensorContext context, String file, String line, String ruleId, String message, Set<String> unresolvedInputFiles) {
    if (ruleId.isEmpty() || message.isEmpty()) {
      LOG.debug("Missing information or unsupported file type for source:'{}', file:'{}', message:'{}'", ruleId, file, message);
      return;
    }
    InputFile inputFile = context.fileSystem().inputFile(context.fileSystem().predicates().hasAbsolutePath(file));
    if (inputFile == null) {
      unresolvedInputFiles.add(file);
      return;
    }

    NewExternalIssue newExternalIssue = context.newExternalIssue();
    newExternalIssue
      .type(ruleLoader().ruleType(ruleId))
      .severity(ruleLoader().ruleSeverity(ruleId))
      .remediationEffortMinutes(ruleLoader().ruleConstantDebtMinutes(ruleId));

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(message)
      .on(inputFile);

    if (!line.isEmpty()) {
      primaryLocation.at(inputFile.selectLine(Integer.parseInt(line)));
    }

    newExternalIssue
      .at(primaryLocation)
      .engineId(propertyKey())
      .ruleId(ruleId)
      .save();
  }

}
