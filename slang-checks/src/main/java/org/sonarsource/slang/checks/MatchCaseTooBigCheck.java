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
package org.sonarsource.slang.checks;

import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import java.text.MessageFormat;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

@Rule(key = "S1151")
public class MatchCaseTooBigCheck implements SlangCheck {

  private static final int DEFAULT_MAX = 15;
  private static final String MESSAGE =
    "Reduce this case clause number of lines from {0} to at most {1}, for example by extracting code into methods.";

  @RuleProperty(
    description = "Maximum number of lines",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public void initialize(InitContext init) {
    init.register(MatchCaseTree.class, (ctx, matchCaseTree) -> {
      int linesOfCode = matchCaseTree.metaData().linesOfCode().size();
      if (linesOfCode > max) {
        ctx.reportIssue(matchCaseTree.rangeToHighlight(), MessageFormat.format(MESSAGE, linesOfCode, max));
      }
    });
  }
}
