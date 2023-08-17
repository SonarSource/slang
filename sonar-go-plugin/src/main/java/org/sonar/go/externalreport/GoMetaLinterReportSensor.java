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

public class GoMetaLinterReportSensor extends AbstractReportSensor {

  private static final Logger LOG = LoggerFactory.getLogger(GoMetaLinterReportSensor.class);

  public static final String PROPERTY_KEY = "sonar.go.gometalinter.reportPaths";

  private static final Pattern GO_META_LINTER_REGEX = Pattern.compile("(?<file>[^:]+):(?<line>\\d+):\\d*:" +
    "(?<severity>(error|warning)):(?<message>.*)\\((?<linter>[^\\(]*)\\)");

  private static final Pattern RULE_KEY_REGEX = Pattern.compile("\\((?<ruleKey>[A-Za-z0-9_-]{1,20})\\)$");

  public GoMetaLinterReportSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, "gometalinter", "GoMetaLinter", PROPERTY_KEY);
  }

  @Nullable
  @Override
  ExternalIssue parse(String line) {
    Matcher matcher = GO_META_LINTER_REGEX.matcher(line);
    if (matcher.matches()) {
      String linter = mapLinterName(matcher.group("linter").trim());
      RuleType type = "error".equals(matcher.group("severity")) ? RuleType.BUG : RuleType.CODE_SMELL;
      String filename = matcher.group("file").trim();
      int lineNumber = Integer.parseInt(matcher.group("line").trim());
      String message = matcher.group("message").trim();
      Matcher ruleKeyMatcher = RULE_KEY_REGEX.matcher(message);
      String ruleKey = null;
      if (ruleKeyMatcher.find()) {
        ruleKey = ruleKeyMatcher.group("ruleKey");
        message = message.substring(0, ruleKeyMatcher.start()).trim();
      }
      return new ExternalIssue(linter, type, ruleKey, filename, lineNumber, message);
    } else {
      LOG.debug("{}Unexpected line: {}", lazyArg(this::logPrefix), line);
    }
    return null;
  }

  private static String mapLinterName(String linter) {
    if ("vet".equals(linter)) {
      return GoVetReportSensor.LINTER_ID;
    }
    return linter;
  }

}
