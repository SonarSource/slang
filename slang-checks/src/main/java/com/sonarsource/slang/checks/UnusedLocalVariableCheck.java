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
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.VariableDeclarationTree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.utils.SyntacticEquivalence;
import org.sonar.check.Rule;

import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S1481")
public class UnusedLocalVariableCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, functionDeclarationTree) -> {

      if(ctx.ancestors().stream().anyMatch(tree -> tree instanceof FunctionDeclarationTree)) {
        return;
      }

      Set<IdentifierTree> variableIdentifiers =
        functionDeclarationTree.descendants()
          .filter(tree -> tree instanceof VariableDeclarationTree)
          .map(VariableDeclarationTree.class::cast)
          .map(VariableDeclarationTree::identifier)
          .collect(Collectors.toSet());

      Set<Tree> identifierTrees =
        functionDeclarationTree.descendants()
          .filter(tree -> !variableIdentifiers.contains(tree))
          .collect(Collectors.toSet());

      variableIdentifiers.stream()
        .filter(var -> identifierTrees.stream().noneMatch(identifier -> SyntacticEquivalence.areEquivalent(var, identifier)))
        .forEach(identifier -> ctx.reportIssue(identifier, "Remove this unused \"" + identifier.name() + "\" local variable."));

    });
  }
}
