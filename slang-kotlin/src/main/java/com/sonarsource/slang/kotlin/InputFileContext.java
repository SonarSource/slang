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
package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.checks.api.SecondaryLocation;
import com.sonarsource.slang.visitors.TreeContext;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;

public class InputFileContext extends TreeContext {

  public final SensorContext sensorContext;

  public final InputFile inputFile;

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
  }

  public TextRange textRange(com.sonarsource.slang.api.TextRange textRange) {
    return inputFile.newRange(
      textRange.start().line(),
      textRange.start().lineOffset(),
      textRange.end().line(),
      textRange.end().lineOffset());
  }

  public void reportIssue(RuleKey ruleKey, com.sonarsource.slang.api.TextRange textRange, String message, List<SecondaryLocation> secondaryLocations) {
    NewIssue issue = sensorContext.newIssue();
    issue
      .forRule(ruleKey)
      .at(issue.newLocation()
        .on(inputFile)
        .at(textRange(textRange))
        .message(message));

    secondaryLocations.forEach(secondary -> issue.addLocation(
      issue.newLocation()
        .on(inputFile)
        .at(textRange(secondary.textRange))
        .message(secondary.message == null ? "" : secondary.message)));

    issue.save();
  }

}
