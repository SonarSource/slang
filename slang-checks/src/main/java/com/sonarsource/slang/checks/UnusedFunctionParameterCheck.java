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

import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.ParameterTree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SecondaryLocation;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonar.check.Rule;

import java.util.List;
import java.util.stream.Collectors;

import static com.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S1172")
public class UnusedFunctionParameterCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, functionDeclarationTree) -> {
      if (!(ctx.parent() instanceof TopLevelTreeImpl)) {
        return;
      }

      if (functionDeclarationTree.body() == null || functionDeclarationTree.body().descendants() == null) {
        return;
      }

      List<ParameterTree> unusedParameters =
          functionDeclarationTree.formalParameters().stream().filter(parameterTree ->
            functionDeclarationTree.body().descendants().noneMatch(tree ->
                areEquivalent(tree, parameterTree.identifier()))).collect(Collectors.toList());

      if (unusedParameters.isEmpty()) {
        return;
      }

      List<SecondaryLocation> secondaryLocations = unusedParameters.stream()
          .skip(1)
          .map(SecondaryLocation::new)
          .collect(Collectors.toList());

      ParameterTree firstUnused = unusedParameters.get(0);
      String msg;

      if (unusedParameters.size() > 1) {
        msg = "Remove these unused function parameters.";
      } else {
        msg = "Remove this unused function parameter \"" + firstUnused.identifier().name() + "\".";
      }

      ctx.reportIssue(firstUnused, msg, secondaryLocations);
    });

  }

}
