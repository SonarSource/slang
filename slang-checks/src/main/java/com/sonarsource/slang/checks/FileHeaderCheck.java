/*
 * SonarSource SLang
 * Copyright (C) 2009-2018 SonarSource SA
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
package com.sonarsource.slang.checks;

import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

@Rule(key = "S1451")
public class FileHeaderCheck implements SlangCheck {

  private static final String MESSAGE = "Add or update the header of this file.";
  private static final String DEFAULT_HEADER_FORMAT = "";

  @RuleProperty(
    key = "headerFormat",
    description = "Expected copyright and license header",
    defaultValue = DEFAULT_HEADER_FORMAT,
    type = "TEXT")
  public String headerFormat = DEFAULT_HEADER_FORMAT;

  @RuleProperty(
    key = "isRegularExpression",
    description = "Whether the headerFormat is a regular expression",
    defaultValue = "false")
  public boolean isRegularExpression = false;
  private Pattern searchPattern = null;
  private String[] expectedLines = null;

  @Override
  public void initialize(InitContext init) {
    initializeParameters();
    init.register(TopLevelTree.class, (ctx, tree) -> {
      if (isRegularExpression) {
        checkRegularExpression(ctx);
      } else {
        checkExpectedLines(ctx);
      }
    });
  }

  private void initializeParameters() {
    if (isRegularExpression) {
      try {
        searchPattern = Pattern.compile(getHeaderFormat(), Pattern.DOTALL);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("[" + getClass().getSimpleName() + "] Unable to compile the regular expression: " + headerFormat, e);
      }
    } else {
      expectedLines = headerFormat.split("(?:\r)?\n|\r");
    }
  }

  private void checkExpectedLines(CheckContext ctx) {
    String[] lines = ctx.fileContent().split("\r\n|\n|\r", -1);
    IntStream.range(0, expectedLines.length)
      .filter(lineIndex -> !lines[lineIndex].equals(expectedLines[lineIndex]))
      .findFirst()
      .ifPresent(lineIndex -> ctx.reportFileIssue(MESSAGE));
  }

  private void checkRegularExpression(CheckContext ctx) {
    Matcher matcher = searchPattern.matcher(ctx.fileContent());
    if (!matcher.find() || matcher.start() != 0) {
      ctx.reportFileIssue(MESSAGE);
    }
  }

  private String getHeaderFormat() {
    String format = headerFormat;
    if(format.charAt(0) != '^') {
      format = "^" + format;
    }
    return format;
  }

}
