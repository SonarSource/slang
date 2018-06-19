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
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

@Rule(key = "S104")
public class TooManyLinesOfCodeFileCheck implements SlangCheck {

  private static final int DEFAULT_MAX = 1000;

  @RuleProperty(
    key = "Max",
    description = "Maximum authorized lines of code in a file.",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> {
      int numberOfLinesOfCode = tree.metaData().linesOfCode().size();
      if (numberOfLinesOfCode > max) {
        String message = String.format(
          "File \"%s\" has %s lines, which is greater than %s authorized. Split it into smaller files.",
          ctx.filename(), numberOfLinesOfCode, max);
        ctx.reportFileIssue(message);
      }
    });
  }

}
