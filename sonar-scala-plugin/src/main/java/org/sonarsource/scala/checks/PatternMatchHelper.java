/*
 * SonarSource SLang
 * Copyright (C) 2018-2023 SonarSource SA
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
package org.sonarsource.scala.checks;

import java.util.Optional;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;

class PatternMatchHelper {

  private static final String PATTERN_VARIABLE = "ScalaNativeKind(class scala.meta.Pat$Var$PatVarImpl)";

  private PatternMatchHelper() {
    // only for static usage
  }

  static boolean hasPatternMatchedVariable(MatchCaseTree caseTree) {
    Tree caseCondition = caseTree.expression();
    Tree caseBody = caseTree.body();
    return caseCondition != null && caseBody != null && caseConditionContainsVariableDeclarationUsedInTheBody(caseCondition, caseBody);
  }

  static boolean caseConditionContainsVariableDeclarationUsedInTheBody(Tree caseCondition, Tree caseBody) {
    if (caseCondition instanceof NativeTree &&
      PATTERN_VARIABLE.equals(((NativeTree) caseCondition).nativeKind().toString())) {
      Optional<IdentifierTree> variableDeclaration = caseCondition.descendants()
        .filter(IdentifierTree.class::isInstance)
        .map(IdentifierTree.class::cast)
        .findFirst();
      return variableDeclaration.filter(variable -> variableIsUsedIn(variable, caseBody)).isPresent();
    }
    return caseCondition.children().stream()
        .anyMatch(caseConditionChild -> caseConditionContainsVariableDeclarationUsedInTheBody(caseConditionChild, caseBody));
  }

  private static boolean variableIsUsedIn(IdentifierTree variableDeclaration, Tree caseBody) {
    if (caseBody instanceof IdentifierTree) {
      return ((IdentifierTree) caseBody).name().equals(variableDeclaration.name());
    }
    return caseBody.children().stream().anyMatch(caseBodyChild -> variableIsUsedIn(variableDeclaration, caseBodyChild));
  }
}
