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
import com.sonarsource.slang.api.IdentifierTree;
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
      if (!(ctx.parent() instanceof TopLevelTreeImpl) || (functionDeclarationTree.body() == null)) {
        return;
      }

      List<ParameterTree> unusedParameters =
          functionDeclarationTree.formalParameters().stream()
            .filter(ParameterTree.class::isInstance)
            .map(ParameterTree.class::cast).filter(parameterTree -> functionDeclarationTree.body().descendants()
              .noneMatch(tree -> areEquivalent(tree, parameterTree.identifier()))).collect(Collectors.toList());

      if (unusedParameters.isEmpty()) {
        return;
      }

      List<SecondaryLocation> secondaryLocations = unusedParameters.stream()
          .map(unusedParameter ->
              new SecondaryLocation(unusedParameter.identifier(), "Remove this unused method parameter " + unusedParameter.identifier().name() + "\"."))
          .collect(Collectors.toList());

      IdentifierTree firstUnused = unusedParameters.get(0).identifier();
      String msg;

      if (unusedParameters.size() > 1) {
        msg = "Remove these unused function parameters.";
      } else {
        msg = "Remove this unused function parameter \"" + firstUnused.name() + "\".";
      }

      ctx.reportIssue(firstUnused, msg, secondaryLocations);
    });

  }

}
