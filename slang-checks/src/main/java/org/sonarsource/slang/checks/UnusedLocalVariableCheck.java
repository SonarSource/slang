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

import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.utils.SyntacticEquivalence;
import org.sonar.check.Rule;

import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S1481")
public class UnusedLocalVariableCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, functionDeclarationTree) -> {

      if(ctx.ancestors().stream().anyMatch(FunctionDeclarationTree.class::isInstance)) {
        return;
      }

      Set<IdentifierTree> variableIdentifiers = getVariableIdentifierTrees(functionDeclarationTree);
      Set<Tree> identifierTrees = getIdentifierTrees(functionDeclarationTree, variableIdentifiers);

      variableIdentifiers.stream()
        .filter(variable -> identifierTrees.stream().noneMatch(identifier -> SyntacticEquivalence.areEquivalent(variable, identifier)))
        .forEach(identifier -> ctx.reportIssue(identifier, "Remove this unused \"" + identifier.name() + "\" local variable."));
    });
  }

  protected Set<IdentifierTree> getVariableIdentifierTrees(FunctionDeclarationTree functionDeclarationTree) {
    return functionDeclarationTree.descendants()
      .filter(VariableDeclarationTree.class::isInstance)
      .map(VariableDeclarationTree.class::cast)
      .map(VariableDeclarationTree::identifier)
      .collect(Collectors.toSet());
  }

  protected Set<Tree> getIdentifierTrees(FunctionDeclarationTree functionDeclarationTree, Set<IdentifierTree> variableIdentifiers) {
    return functionDeclarationTree.descendants()
      .filter(tree -> !variableIdentifiers.contains(tree))
      .collect(Collectors.toSet());
  }
}
