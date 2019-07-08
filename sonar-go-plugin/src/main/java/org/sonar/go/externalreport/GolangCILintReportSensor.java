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
package org.sonar.go.externalreport;

import java.io.File;
import java.util.List;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.go.plugin.GoLanguage;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.slang.externalreport.CheckstyleFormatImporter;

public class GolangCILintReportSensor implements Sensor {

  public static final String LINTER_KEY = "golangci-lint";

  public static final String PROPERTY_KEY = "sonar.go.golangci-lint.reportPaths";

  @Override
  public void describe(SensorDescriptor sensorDescriptor) {
    sensorDescriptor
      .onlyOnLanguage(GoLanguage.KEY)
      .onlyWhenConfiguration(conf -> conf.hasKey(PROPERTY_KEY))
      .name("Import of GolangCI-Lint issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, PROPERTY_KEY);
    CheckstyleFormatImporter importer = new CheckstyleFormatImporter(context, LINTER_KEY);
    reportFiles.forEach(importer::importFile);
  }

}
