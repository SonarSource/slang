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
package com.sonarsource.slang.checks.commentedcode;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.CodeVerifier;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.parser.SLangConverter;

public class SlangCodeVerifier implements CodeVerifier {
  @Override
  public boolean containsCode(String content) {
    SLangConverter sLangConverter = new SLangConverter();
    Tree tree;
    try {
      tree = sLangConverter.parse(content);
    } catch (Exception e) {
      tree = null;
    }

    return tree != null && !isSimpleExpression(tree);
  }

  private static boolean isSimpleExpression(Tree tree) {
    long all = tree.descendants().count();
    if (all == 0) {
      return true;
    }
    long remaining = tree.descendants()
      .filter(element -> !(element instanceof IdentifierTree ||
        element instanceof LiteralTree ||
        simpleBinaryExpressionTree(element)))
      .count();

    double percentage = (double) remaining / all;
    return percentage < 0.3;

  }

  private static boolean simpleBinaryExpressionTree(Tree element) {
    if (element instanceof BinaryExpressionTree) {
      BinaryExpressionTree expression = (BinaryExpressionTree) element;
      return expression.operator().equals(BinaryExpressionTree.Operator.PLUS)
        || expression.operator().equals(BinaryExpressionTree.Operator.MINUS)
        || expression.operator().equals(BinaryExpressionTree.Operator.DIVIDED_BY);
    }
    return false;
  }
}
