/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.checks;


import org.sonar.check.Rule;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S131")
public class MatchWithoutElseCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(MatchTree.class, (ctx, tree) -> {
      if (tree.cases().stream().noneMatch(matchCase -> matchCase.expression() == null)) {
        Token keyword = tree.keyword();
        String message = String.format("Add a default clause to this \"%s\" statement.", keyword.text());
        ctx.reportIssue(keyword, message);
      }
    });
  }

}
