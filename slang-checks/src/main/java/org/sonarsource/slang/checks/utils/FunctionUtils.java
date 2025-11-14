/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.checks.utils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.MemberSelectTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.api.Tree;

import static org.sonarsource.slang.api.ModifierTree.Kind.OVERRIDE;
import static org.sonarsource.slang.api.ModifierTree.Kind.PRIVATE;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.getMemberSelectOrIdentifierName;

public class FunctionUtils {

  private FunctionUtils() {
  }

  public static boolean isPrivateMethod(FunctionDeclarationTree method) {
    return hasModifierMethod(method, PRIVATE);

  }

  public static boolean isOverrideMethod(FunctionDeclarationTree method) {
    return hasModifierMethod(method, OVERRIDE);
  }

  public static boolean hasModifierMethod(FunctionDeclarationTree method, ModifierTree.Kind kind) {
    return method.modifiers().stream()
      .filter(ModifierTree.class::isInstance)
      .map(ModifierTree.class::cast)
      .anyMatch(modifier -> modifier.kind() == kind);
  }

  public static boolean hasFunctionCallNameIgnoreCase(FunctionInvocationTree tree, String name) {
    return getFunctionInvocationName(tree).filter(name::equalsIgnoreCase).isPresent();
  }

  public static Set<String> getStringsTokens(FunctionDeclarationTree functionDeclarationTree, String delimitersRegex) {
    Set<String> stringLiteralTokens = new HashSet<>();
    functionDeclarationTree.descendants()
      .filter(StringLiteralTree.class::isInstance)
      .map(StringLiteralTree.class::cast)
      .map(StringLiteralTree::content)
      .forEach(literal -> stringLiteralTokens.addAll(Arrays.asList(literal.split(delimitersRegex))));
    return stringLiteralTokens;
  }

  private static Optional<String> getFunctionInvocationName(FunctionInvocationTree tree) {
    return getMemberSelectOrIdentifierName(tree.memberSelect());
  }

  public static boolean hasFunctionCallFullNameIgnoreCase(FunctionInvocationTree tree, String... names) {
    return hasFunctionCallFullNameIgnoreCaseHelper(tree.memberSelect(), Arrays.asList(names));
  }

  private static boolean hasFunctionCallFullNameIgnoreCaseHelper(Tree tree, List<String> names) {
    if (tree instanceof IdentifierTree) {
      return names.size() == 1 && ((IdentifierTree) tree).name().equalsIgnoreCase(names.get(0));
    } else if (tree instanceof MemberSelectTree) {
      MemberSelectTree memberSelectTree = (MemberSelectTree) tree;
      return names.size() > 1
        && memberSelectTree.identifier().name().equalsIgnoreCase(names.get(names.size()-1))
        && hasFunctionCallFullNameIgnoreCaseHelper(memberSelectTree.expression(), dropLastElement(names));
    } else {
      // Any other node (native, ...): we don't known anything about them!
      return false;
    }
  }

  private static List<String> dropLastElement(List<String> list) {
    return list.subList(0, list.size()-1);
  }
}
