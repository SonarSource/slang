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
package com.sonarsource.slang.checks.utils;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.Tree;

public class SyntacticEquivalence {

  private SyntacticEquivalence() {
  }

  public static boolean areEquivalent(Tree first, Tree second) {
    if (!first.getClass().equals(second.getClass())) {
      return false;
    }

    if (first instanceof IdentifierTree) {
      return ((IdentifierTree) first).name().equals(((IdentifierTree) second).name());
    } else if (first instanceof LiteralTree) {
      return ((LiteralTree) first).value().equals(((LiteralTree) second).value());
    } else if (first instanceof NativeTree) {
      if (!((NativeTree) first).nativeKind().equals(((NativeTree) second).nativeKind())) {
        return false;
      }
    } else if (first instanceof BinaryExpressionTree) {
      if (((BinaryExpressionTree) first).operator() != ((BinaryExpressionTree) second).operator()) {
        return false;
      }
    }

    if (first.children().size() != second.children().size()) {
      return false;
    }

    for (int i = 0; i < first.children().size(); i++) {
      if (!areEquivalent(first.children().get(i), second.children().get(i))) {
        return false;
      }
    }

    return true;
  }
}
