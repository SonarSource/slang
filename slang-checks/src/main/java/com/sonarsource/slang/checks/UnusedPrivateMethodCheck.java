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
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;

import static com.sonarsource.slang.api.ModifierTree.Kind.PRIVATE;

@Rule(key = "S1144")
public class UnusedPrivateMethodCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(ClassDeclarationTree.class, (ctx, classDeclarationTree) -> {
      Set<FunctionDeclarationTree> classMethods = classDeclarationTree.descendants()
        .filter(FunctionDeclarationTree.class::isInstance)
        .map(FunctionDeclarationTree.class::cast)
        .collect(Collectors.toSet());
      Set<IdentifierTree> usedIdentifiers = classDeclarationTree.descendants()
        .filter(IdentifierTree.class::isInstance)
        .map(IdentifierTree.class::cast)
        .collect(Collectors.toSet());

      usedIdentifiers.removeAll(classMethods.stream()
        .map(FunctionDeclarationTree::name)
        .collect(Collectors.toSet()));
      usedIdentifiers.remove(classDeclarationTree.identifier());

      Set<String> usedIdentifierNames = usedIdentifiers.stream()
        .map(IdentifierTree::name)
        .collect(Collectors.toSet());

      classMethods.stream()
        .filter(UnusedPrivateMethodCheck::isPrivateMethod)
        .forEach(tree -> {
          IdentifierTree identifier = tree.name();
          if (identifier != null && !usedIdentifierNames.contains(identifier.name())) {
            String message = String.format("Remove this unused private \"%s\" method.", identifier.name());
            ctx.reportIssue(tree.rangeToHighlight(), message);
          }
        });

    });

  }

  private static boolean isPrivateMethod(FunctionDeclarationTree method) {
    return method.modifiers().stream()
      .filter(ModifierTree.class::isInstance)
      .map(ModifierTree.class::cast)
      .anyMatch(modifier -> modifier.kind() == PRIVATE);
  }

}
