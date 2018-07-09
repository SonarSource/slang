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

import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.LoopTree;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.function.BiConsumer;
import org.sonar.check.Rule;

@Rule(key = "S121")
public class MissingCurlyBracesCheck implements SlangCheck {

  private static final String MESSAGE = "Add curly braces around the nested statement(s) in this \"{0}\" block.";

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, filterStatement(MissingCurlyBracesCheck::checkIfStatement));
    init.register(LoopTree.class, filterStatement((ctx, loop) -> checkStatement(ctx, loop.keyword(), loop.body())));
  }

  private static <T extends Tree> BiConsumer<CheckContext, T> filterStatement(BiConsumer<CheckContext, T> consumer) {
    return (ctx, tree) -> {
      // this way of guessing "statement" will be improved by SONARSLANG-92
      if (parentIsBlockButNotLambda(ctx) || ctx.parent() instanceof TopLevelTree) {
        consumer.accept(ctx, tree);
      }
    };
  }

  private static void checkIfStatement(CheckContext ctx, IfTree ifStatement) {
    checkStatement(ctx, ifStatement.ifKeyword(), ifStatement.thenBranch());
    Tree elseBranch = ifStatement.elseBranch();
    Token elseKeyword = ifStatement.elseKeyword();
    if (elseBranch != null && elseKeyword != null && !(elseBranch instanceof IfTree)) {
      checkStatement(ctx, elseKeyword, elseBranch);
    }
  }

  private static void checkStatement(CheckContext ctx, Token reportToken, Tree statement) {
    if (!(statement instanceof BlockTree)) {
      ctx.reportIssue(reportToken, MessageFormat.format(MESSAGE, reportToken.text()));
    }
  }

  private static boolean parentIsBlockButNotLambda(CheckContext ctx) {
    if (ctx.parent() instanceof BlockTree) {
      Iterator<Tree> iterator = ctx.ancestors().iterator();
      iterator.next();
      return !isLambda(iterator.next());
    }
    return false;
  }

  private static boolean isLambda(Tree tree) {
    return tree instanceof FunctionDeclarationTree && ((FunctionDeclarationTree) tree).name() == null;
  }

}
