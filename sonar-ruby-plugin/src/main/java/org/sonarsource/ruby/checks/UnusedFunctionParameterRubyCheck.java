/*
 * SonarSource SLang
 * Copyright (C) 2018-2020 SonarSource SA
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
package org.sonarsource.ruby.checks;

import java.util.List;
import java.util.Set;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.checks.UnusedFunctionParameterCheck;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.utils.FunctionUtils;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.TopLevelTreeImpl;

public class UnusedFunctionParameterRubyCheck extends UnusedFunctionParameterCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, functionDeclarationTree) -> {
      if (functionDeclarationTree.isConstructor() || shouldBeIgnored(ctx, functionDeclarationTree)) {
        return;
      }

      List<ParameterTree> unusedParameters = getUnusedParameters(functionDeclarationTree);

      if (unusedParameters.isEmpty()) {
        return;
      }

      // the unused parameters may actually be used inside interpolated strings, eval or prepared statements
      Set<String> stringLiteralTokens = FunctionUtils.getStringsTokens(functionDeclarationTree, Constants.SPECIAL_STRING_DELIMITERS);
      unusedParameters.stream()
        .filter(var -> !stringLiteralTokens.contains(var.identifier().name()))
        .forEach(identifier -> reportUnusedParameters(ctx, unusedParameters));
    });
  }

  @Override
  protected boolean isValidFunctionForRule(CheckContext ctx, FunctionDeclarationTree tree) {
    return parentIsBlockUnderTopLevel(ctx) || super.isValidFunctionForRule(ctx, tree);
  }

  private static boolean parentIsBlockUnderTopLevel(CheckContext ctx) {
    return ctx.parent() instanceof BlockTreeImpl &&
      ctx.ancestors().size() == 2 &&
      ctx.ancestors().getLast() instanceof TopLevelTreeImpl;
  }

}
