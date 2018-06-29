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

import com.sonarsource.slang.api.ClassDeclarationTree;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.ModifierTree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;

import static com.sonarsource.slang.api.ModifierTree.Kind.PRIVATE;

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

      Set<IdentifierTree> usedIdentifiers = classDeclarationTree.descendants()
        .filter(IdentifierTree.class::isInstance)
        .map(IdentifierTree.class::cast)
        .collect(Collectors.toSet());

      usedIdentifiers.removeAll(classMethods.stream()
        .map(FunctionDeclarationTree::name)
        .collect(Collectors.toSet()));

      Set<String> usedIdentifierNames = usedIdentifiers.stream()
        .map(IdentifierTree::name)
        .collect(Collectors.toSet());

      classMethods.stream()
        .filter(UnusedPrivateMethodCheck::isPrivateMethod)
        .forEach(tree -> {
          IdentifierTree identifier = tree.name();
          if (isUnusedMethod(identifier, usedIdentifierNames)) {
            String message = String.format("Remove this unused private \"%s\" method.", identifier.name());
            ctx.reportIssue(tree.rangeToHighlight(), message);
          }
        });

    });

  }

  private static boolean isUnusedMethod(@Nullable IdentifierTree identifier, Set<String> usedIdentifierNames) {
    return identifier != null
      && !usedIdentifierNames.contains(identifier.name())
      && !IGNORED_METHODS.contains(identifier.name());
  }

  private static boolean isPrivateMethod(FunctionDeclarationTree method) {
    return method.modifiers().stream()
      .filter(ModifierTree.class::isInstance)
      .map(ModifierTree.class::cast)
      .anyMatch(modifier -> modifier.kind() == PRIVATE);
  }

}
