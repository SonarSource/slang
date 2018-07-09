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

import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;

import static org.sonarsource.slang.checks.utils.FunctionUtils.isPrivateMethod;
import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S1172")
public class UnusedFunctionParameterCheck implements SlangCheck {

  // Currently we ignore all functions named "main", however this should be configurable based on the analyzed language in the future.
  private static final Pattern IGNORED_PATTERN = Pattern.compile("main", Pattern.CASE_INSENSITIVE);

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, functionDeclarationTree) -> {
      if (shouldBeIgnored(ctx, functionDeclarationTree)) {
        return;
      }

      List<ParameterTree> unusedParameters =
          functionDeclarationTree.formalParameters().stream()
            .filter(ParameterTree.class::isInstance)
            .map(ParameterTree.class::cast).filter(parameterTree -> functionDeclarationTree.descendants()
              .noneMatch(tree -> !tree.equals(parameterTree.identifier()) && areEquivalent(tree, parameterTree.identifier())))
            .collect(Collectors.toList());

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

  private static boolean shouldBeIgnored(CheckContext ctx, FunctionDeclarationTree tree) {
    IdentifierTree name = tree.name();
    boolean validFunctionForRule = ctx.parent() instanceof TopLevelTreeImpl || isPrivateMethod(tree);
    return !validFunctionForRule
      || tree.body() == null
      || (name != null && IGNORED_PATTERN.matcher(name.name()).matches());
  }

}
