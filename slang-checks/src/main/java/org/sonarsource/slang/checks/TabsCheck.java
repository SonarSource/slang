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

import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;

@Rule(key = "S105")
public class TabsCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> {
      String fileContent = ctx.fileContent();
      if (fileContent.contains("\t")) {
        String message = String.format("Replace all tab characters in this file \"%s\" by sequences of white-spaces.", ctx.filename());
        ctx.reportFileIssue(message);
      }
    });
  }

}
