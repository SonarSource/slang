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
package org.sonar.go.externalreport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rules.RuleType;

import static org.sonarsource.slang.utils.LogArg.lazyArg;

public class GoLintReportSensor extends AbstractReportSensor {

  private static final Logger LOG = LoggerFactory.getLogger(GoLintReportSensor.class);

  public static final String PROPERTY_KEY = "sonar.go.golint.reportPaths";

  private static final Pattern GO_LINT_LINE_REGEX = Pattern.compile("(?<file>[^:]+):(?<line>\\d+):\\d*:(?<message>.*)");

  public static final String LINTER_ID = "golint";
  public static final String LINTER_NAME = "Golint";

  public GoLintReportSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, LINTER_ID, LINTER_NAME, PROPERTY_KEY);
  }

  @Nullable
  @Override
  ExternalIssue parse(String line) {
    Matcher matcher = GO_LINT_LINE_REGEX.matcher(line);
    if (matcher.matches()) {
      String filename = matcher.group("file").trim();
      int lineNumber = Integer.parseInt(matcher.group("line").trim());
      String message = matcher.group("message").trim();
      return new ExternalIssue(LINTER_ID, RuleType.CODE_SMELL, null, filename, lineNumber, message);
    } else {
      LOG.debug("{}Unexpected line: {}", lazyArg(this::logPrefix), line);
    }
    return null;
  }

}
