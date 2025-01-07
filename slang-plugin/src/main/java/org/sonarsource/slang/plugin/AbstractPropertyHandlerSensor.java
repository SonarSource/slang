/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.plugin;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonarsource.analyzer.commons.ExternalReportProvider;

public abstract class AbstractPropertyHandlerSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractPropertyHandlerSensor.class);
  private final AnalysisWarnings analysisWarnings;
  private final String propertyKey;
  private final String propertyName;
  private final String configurationKey;
  private final String languageKey;

  protected AbstractPropertyHandlerSensor(AnalysisWarnings analysisWarnings, String propertyKey, String propertyName,
                                          String configurationKey, String languageKey) {
    this.analysisWarnings = analysisWarnings;
    this.propertyKey = propertyKey;
    this.propertyName = propertyName;
    this.configurationKey = configurationKey;
    this.languageKey = languageKey;
  }

  public final String propertyName() {
    return propertyName;
  }

  public final String propertyKey() {
    return propertyKey;
  }

  public final String configurationKey() {
    return configurationKey;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(languageKey)
      .onlyWhenConfiguration(conf -> conf.hasKey(configurationKey()))
      .name("Import of " + propertyName() + " issues");
  }

  @Override
  public void execute(SensorContext context) {
    executeOnFiles(reportFiles(context), reportConsumer(context));
  }

  public abstract Consumer<File> reportConsumer(SensorContext context);

  private void executeOnFiles(List<File> reportFiles, Consumer<File> action) {
    reportFiles.stream()
      .filter(File::exists)
      .forEach(file -> {
        LOG.info("Importing {}", file);
        action.accept(file);
      });
    reportMissingFiles(reportFiles);
  }

  private List<File> reportFiles(SensorContext context) {
    return ExternalReportProvider.getReportFiles(context, configurationKey());
  }

  private void reportMissingFiles(List<File> reportFiles) {
    List<String> missingFiles = reportFiles.stream()
      .filter(file -> !file.exists())
      .map(File::getPath)
      .toList();

    if (!missingFiles.isEmpty()) {
      String missingFilesAsString = missingFiles.stream().collect(Collectors.joining("\n- ", "\n- ", ""));
      String logWarning = String.format("Unable to import %s report file(s):%s%nThe report file(s) can not be found. Check that the property '%s' is correctly configured.",
        propertyName(), missingFilesAsString, configurationKey());
      LOG.warn(logWarning);

      String uiWarning = String.format("Unable to import %d %s report file(s).%nPlease check that property '%s' is correctly configured and the analysis logs for more details.",
        missingFiles.size(), propertyName(), configurationKey());
      analysisWarnings.addUnique(uiWarning);
    }
  }
}
