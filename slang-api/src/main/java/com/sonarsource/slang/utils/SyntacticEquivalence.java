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
package com.sonarsource.slang.utils;

import com.sonarsource.slang.api.AssignmentExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.ParameterTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.visitors.TreePrinter;

import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class SyntacticEquivalence {

  private SyntacticEquivalence() {
  }

  public static boolean areEquivalent(@Nullable List<Tree> first, @Nullable List<Tree> second) {
    if (first == second) {
      return true;
    }

    if (first == null || second == null || first.size() != second.size()) {
      return false;
    }

    for (int i = 0; i < first.size(); i++) {
      if (!areEquivalent(first.get(i), second.get(i))) {
        return false;
      }
    }

    return true;
  }

  public static boolean areEquivalent(@Nullable Tree first, @Nullable Tree second) {
    if (first == second) {
      return true;
    }

    if (first == null || second == null || !first.getClass().equals(second.getClass())) {
      return false;
    }

    if (first instanceof IdentifierTree) {
      return ((IdentifierTree) first).name().equals(((IdentifierTree) second).name());
    } else if (first instanceof LiteralTree) {
      return ((LiteralTree) first).value().equals(((LiteralTree) second).value());
    } else if (first instanceof ParameterTree) {
      return ((ParameterTree) first).equals((ParameterTree) (second));
    } else if (first instanceof NativeTree) {
      if (!((NativeTree) first).nativeKind().equals(((NativeTree) second).nativeKind())) {
        return false;
      }
    } else if (first instanceof BinaryExpressionTree) {
      if (((BinaryExpressionTree) first).operator() != ((BinaryExpressionTree) second).operator()) {
        return false;
      }
    } else if (first instanceof AssignmentExpressionTree
      && (((AssignmentExpressionTree) first).operator() != ((AssignmentExpressionTree) second).operator())) {
      return false;
    }

    return areEquivalent(first.children(), second.children());
  }

  public static List<List<Tree>> findDuplicatedGroups(List<Tree> list) {
    return list.stream()
      .collect(Collectors.groupingBy(ComparableTree::new, LinkedHashMap::new, Collectors.toList()))
      .values().stream()
      .filter(group -> group.size() > 1)
      .collect(Collectors.toList());
  }

  static class ComparableTree {

    private final Tree tree;
    private final int hash;

    ComparableTree(Tree tree) {
      this.tree = tree;
      hash = computeHash(tree);
    }

    private static int computeHash(@Nullable Tree tree) {
      if (tree == null) {
        return 0;
      }
      return TreePrinter.tree2string(tree).hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof ComparableTree)) {
        return false;
      }
      ComparableTree that = (ComparableTree) other;
      return hash == that.hash && areEquivalent(tree, ((ComparableTree) other).tree);
    }

    @Override
    public int hashCode() {
      return hash;
    }

  }
}
