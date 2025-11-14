/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S108")
public class EmptyBlockCheck implements SlangCheck {

  private static final String MESSAGE = "Either remove or fill this block of code.";

  @Override
  public void initialize(InitContext init) {
    init.register(BlockTree.class, (ctx, blockTree) -> {
      Tree parent = ctx.parent();
      if (isValidBlock(parent) && blockTree.statementOrExpressions().isEmpty()) {
        checkComments(ctx, blockTree);
      }
    });

    init.register(MatchTree.class, (ctx, matchTree) -> {
      if (matchTree.cases().isEmpty()) {
        checkComments(ctx, matchTree);
      }
    });
  }

  private static boolean isValidBlock(@Nullable Tree parent) {
    return !(parent instanceof FunctionDeclarationTree)
      && !(parent instanceof NativeTree)
      && !isWhileLoop(parent);
  }

  private static boolean isWhileLoop(@Nullable Tree parent) {
    return parent instanceof LoopTree && ((LoopTree) parent).kind() == LoopTree.LoopKind.WHILE;
  }

  private static void checkComments(CheckContext ctx, Tree tree) {
    if (tree.metaData().commentsInside().isEmpty()) {
      ctx.reportIssue(tree, MESSAGE);
    }
  }

}
