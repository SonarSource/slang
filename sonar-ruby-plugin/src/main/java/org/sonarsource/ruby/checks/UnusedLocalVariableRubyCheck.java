package org.sonarsource.ruby.checks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.UnusedLocalVariableCheck;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.utils.SyntacticEquivalence;

public class UnusedLocalVariableRubyCheck extends UnusedLocalVariableCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, functionDeclarationTree) -> {

      if (ctx.ancestors().stream().anyMatch(tree -> tree instanceof FunctionDeclarationTree)) {
        return;
      }

      Set<IdentifierTree> variableIdentifiers = getVariableIdentifierTrees(functionDeclarationTree);
      Set<Tree> identifierTrees = getIdentifierTrees(functionDeclarationTree, variableIdentifiers);

      List<IdentifierTree> unusedVariables = variableIdentifiers.stream()
        .filter(var -> identifierTrees.stream().noneMatch(identifier -> SyntacticEquivalence.areEquivalent(var, identifier)))
        .collect(Collectors.toList());

      if (unusedVariables.isEmpty()) {
        return;
      }

      // the unused variables may actually be used inside interpolated strings, eval or prepared statements
      Set<String> stringLiteralTokens = getStringsTokens(functionDeclarationTree);
      unusedVariables.stream()
        .filter(var -> !stringLiteralTokens.contains(var.name()))
        .forEach(identifier -> ctx.reportIssue(identifier, "Remove this unused \"" + identifier.name() + "\" local variable."));
    });
  }

  private static Set<String> getStringsTokens(FunctionDeclarationTree functionDeclarationTree) {
    Set<String> stringLiteralTokens = new HashSet<>();
    functionDeclarationTree.descendants()
      .filter(StringLiteralTree.class::isInstance)
      .map(StringLiteralTree.class::cast)
      .map(StringLiteralTree::content)
      .forEach(literal -> stringLiteralTokens.addAll(Arrays.asList(literal.split("\\s|#\\{|}|:"))));
    return stringLiteralTokens;
  }

}
