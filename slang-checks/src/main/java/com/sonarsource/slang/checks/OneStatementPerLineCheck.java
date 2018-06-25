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
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SecondaryLocation;
import com.sonarsource.slang.checks.api.SlangCheck;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;

@Rule(key = "S122")
public class OneStatementPerLineCheck implements SlangCheck {
  private static final String MESSAGE = "Reformat the code to have only one statement per line.";

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, topLevelTree) -> checkStatements(ctx, topLevelTree.children()));
    init.register(BlockTree.class, (ctx, blockTree) -> checkStatements(ctx, blockTree.statementOrExpressions()));
  }

  private static void checkStatements(CheckContext ctx, List<Tree> statementsOrExpressions) {
    statementsOrExpressions.stream()
      .collect(Collectors.groupingBy(OneStatementPerLineCheck::getLine))
      .forEach((line, statements) -> {
        if (statements.size() > 1) {
          reportIssue(ctx, statements);
        }
      });
  }

  private static void reportIssue(CheckContext ctx, List<Tree> statements) {
    List<SecondaryLocation> secondaryLocations = statements.stream()
      .skip(2)
      .map(statement -> new SecondaryLocation(statement, null))
      .collect(Collectors.toList());
    ctx.reportIssue(
      statements.get(1),
      MESSAGE,
      secondaryLocations
    );
  }

  private static int getLine(Tree statementOrExpression) {
    return statementOrExpression.metaData().textRange().start().line();
  }
}
