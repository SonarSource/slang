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

import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.checks.utils.FunctionUtils;
import org.sonarsource.slang.visitors.TreeContext;
import org.sonarsource.slang.visitors.TreeVisitor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S1144")
public class UnusedPrivateMethodCheck implements SlangCheck {

  // Serializable method should not raise any issue in Kotlin. Either change it as parameter when adding new language,
  // or add all exceptions here
  private static final Set<String> IGNORED_METHODS = new HashSet<>(Arrays.asList(
    "writeObject",
    "readObject",
    "writeReplace",
    "readResolve",
    "readObjectNoData"));

  @Override
  public void initialize(InitContext init) {
    init.register(ClassDeclarationTree.class, (ctx, classDeclarationTree) -> {
      Set<FunctionDeclarationTree> classMethods = new HashSet<>();
      TreeVisitor<TreeContext> functionVisitor = new TreeVisitor<>();
      functionVisitor.register(FunctionDeclarationTree.class,
        (functionCtx, functionDeclarationTree) -> {
          boolean isCurrentClassMethod = functionCtx.ancestors().stream()
            .filter(ClassDeclarationTree.class::isInstance)
            .findFirst().map(classDeclarationTree::equals)
            .orElse(false);
          if (isCurrentClassMethod) {
            classMethods.add(functionDeclarationTree);
          }
        });
      functionVisitor.scan(new TreeContext(), classDeclarationTree);

      Set<IdentifierTree> usedIdentifiers = getAllUsedIdentifiers(ctx, classDeclarationTree);

      usedIdentifiers.removeAll(classMethods.stream()
        .map(FunctionDeclarationTree::name)
        .collect(Collectors.toSet()));

      classMethods.stream()
        .filter(method -> FunctionUtils.isPrivateMethod(method) && !FunctionUtils.isOverrideMethod(method))
        .forEach(tree -> {
          IdentifierTree identifier = tree.name();
          if (isUnusedMethod(identifier, usedIdentifiers)) {
            String message = String.format("Remove this unused private \"%s\" method.", identifier.name());
            ctx.reportIssue(tree.rangeToHighlight(), message);
          }
        });

    });

  }

  private static Set<IdentifierTree> getAllUsedIdentifiers(CheckContext ctx, ClassDeclarationTree classDeclarationTree) {
    ClassDeclarationTree topLevelClassDeclarationTree = ctx.ancestors().stream()
      .filter(ClassDeclarationTree.class::isInstance)
      .map(ClassDeclarationTree.class::cast)
      .reduce((first, last) -> last)
      .orElse(classDeclarationTree);

    return topLevelClassDeclarationTree.descendants()
      .filter(IdentifierTree.class::isInstance)
      .map(IdentifierTree.class::cast)
      .collect(Collectors.toSet());
  }

  private static boolean isUnusedMethod(@Nullable IdentifierTree identifier, Set<IdentifierTree> usedIdentifierNames) {
    return identifier != null
      && usedIdentifierNames.stream().noneMatch(usedIdentifier -> areEquivalent(identifier, usedIdentifier))
      && !IGNORED_METHODS.contains(identifier.name());
  }

}
