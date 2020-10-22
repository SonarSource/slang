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
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rules.RuleType;
import org.sonar.go.plugin.GoLanguage;
import org.sonarsource.slang.externalreport.CheckstyleFormatImporter;
import org.sonarsource.slang.plugin.AbstractPropertyHandlerSensor;

public class GolangCILintReportSensor extends AbstractPropertyHandlerSensor {

  public static final String LINTER_KEY = "golangci-lint";
  public static final String LINTER_NAME = "GolangCI-Lint";

  public static final String PROPERTY_KEY = "sonar.go.golangci-lint.reportPaths";

  public GolangCILintReportSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, LINTER_KEY, LINTER_NAME, PROPERTY_KEY, GoLanguage.KEY);
  }

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return new GolangCILintCheckstyleFormatImporter(context, LINTER_KEY)::importFile;
  }

  static class GolangCILintCheckstyleFormatImporter extends CheckstyleFormatImporter {

    public GolangCILintCheckstyleFormatImporter(SensorContext context, String linterKey) {
      super(context, linterKey);
    }

    @Override
    protected RuleType ruleType(String ruleKey, @Nullable String severity, String source) {
      if ("gosec".equals(source)) {
        return RuleType.VULNERABILITY;
      }
      return super.ruleType(ruleKey, severity, source);
    }

  }
}
