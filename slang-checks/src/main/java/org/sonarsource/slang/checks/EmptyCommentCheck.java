/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource Sàrl
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

import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;

@Rule(key = "S4663")
public class EmptyCommentCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) ->
      tree.allComments().stream()
        .filter(comment -> comment.contentText().trim().isEmpty() && !comment.contentRange().end().equals(comment.textRange().end()))
        .forEach(comment -> ctx.reportIssue(comment, "Remove this comment, it is empty.")));
  }

}
