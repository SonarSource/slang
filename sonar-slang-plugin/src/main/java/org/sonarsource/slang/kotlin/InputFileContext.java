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
package org.sonarsource.slang.kotlin;

import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.visitors.TreeContext;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

public class InputFileContext extends TreeContext {

  public final SensorContext sensorContext;

  public final InputFile inputFile;

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
  }

  public TextRange textRange(org.sonarsource.slang.api.TextRange textRange) {
    return inputFile.newRange(
      textRange.start().line(),
      textRange.start().lineOffset(),
      textRange.end().line(),
      textRange.end().lineOffset());
  }

  public void reportIssue(RuleKey ruleKey,
                          @Nullable org.sonarsource.slang.api.TextRange textRange,
                          String message,
                          List<SecondaryLocation> secondaryLocations,
                          @Nullable Double gap) {
    NewIssue issue = sensorContext.newIssue();
    NewIssueLocation issueLocation = issue.newLocation()
      .on(inputFile)
      .message(message);

    if (textRange != null) {
      issueLocation.at(textRange(textRange));
    }

    issue
      .forRule(ruleKey)
      .at(issueLocation)
      .gap(gap);

    secondaryLocations.forEach(secondary -> issue.addLocation(
      issue.newLocation()
        .on(inputFile)
        .at(textRange(secondary.textRange))
        .message(secondary.message == null ? "" : secondary.message)));

    issue.save();
  }

  public void reportError(String message, @Nullable org.sonarsource.slang.api.TextPointer location) {
    NewAnalysisError error = sensorContext.newAnalysisError();
    error
      .message(message)
      .onFile(inputFile);

    if (location != null) {
      TextPointer pointerLocation = inputFile.newPointer(location.line(), location.lineOffset());
      error.at(pointerLocation);
    }

    error.save();
  }

}
