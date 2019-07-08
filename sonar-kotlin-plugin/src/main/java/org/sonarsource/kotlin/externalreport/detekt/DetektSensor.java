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
package org.sonarsource.kotlin.externalreport.detekt;

import java.io.File;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.kotlin.plugin.KotlinPlugin;
import org.sonarsource.slang.externalreport.CheckstyleFormatImporterWithRuleLoader;

public class DetektSensor implements Sensor {

  private static final Logger LOG = Loggers.get(DetektSensor.class);

  static final String LINTER_KEY = "detekt";

  static final String LINTER_NAME = "detekt";

  private static final String DETEKT_PREFIX = "detekt.";

  public static final String REPORT_PROPERTY_KEY = "sonar.kotlin.detekt.reportPaths";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(KotlinPlugin.KOTLIN_LANGUAGE_KEY)
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .name("Import of detekt issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    ReportImporter importer = new ReportImporter(context);
    reportFiles.forEach(importer::importFile);
  }

  private static class ReportImporter extends CheckstyleFormatImporterWithRuleLoader {

    public ReportImporter(SensorContext context) {
      super(context, LINTER_KEY, DetektRulesDefinition.RULE_LOADER);
    }

    @Override
    @Nullable
    protected RuleKey createRuleKey(String source) {
      if (!source.startsWith(DETEKT_PREFIX)) {
        LOG.debug("Unexpected rule key without '{}' suffix: '{}'", DETEKT_PREFIX, source);
        return null;
      }
      return super.createRuleKey(source.substring(DETEKT_PREFIX.length()));
    }

  }

}
