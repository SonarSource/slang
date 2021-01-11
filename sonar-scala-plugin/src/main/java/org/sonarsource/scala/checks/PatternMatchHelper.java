/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import javax.annotation.Nullable;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.NativeTreeImpl;

class PatternMatchHelper {
  // e.g. "case Some(value)"
  private static final String PATTERN_EXTRACT_TYPE = "scala.meta.Pat$Extract$PatExtractImpl";
  // e.g. "case d: JavaBigDecimal"
  private static final String PATTERN_INSTANCEOF_TYPE = "scala.meta.Pat$Typed$PatTypedImpl";

  private PatternMatchHelper() {
    // only for static usage
  }

  static boolean hasPatternMatchedVariable(MatchCaseTree caseTree) {
    Tree expression = caseTree.expression();
    if (isNativeKind(expression, PATTERN_EXTRACT_TYPE) || isNativeKind(expression, PATTERN_INSTANCEOF_TYPE)) {
      return expression.descendants().anyMatch(c -> isNativeKind(c, "scala.meta.Pat$Var$PatVarImpl"));
    }
    return false;
  }

  private static boolean isNativeKind(@Nullable Tree tree, String nativeType) {
    if (tree instanceof NativeTreeImpl) {
      NativeTreeImpl nativeTree = (NativeTreeImpl)tree;
      return nativeTree.nativeKind().toString().contains(nativeType);
    }
    return false;
  }
}
