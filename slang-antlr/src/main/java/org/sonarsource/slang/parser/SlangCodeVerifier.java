/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.parser;

import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.CodeVerifier;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.Tree;

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

    double detectedCodeRatio = (double) remaining / all;
    return detectedCodeRatio < 0.3;

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
