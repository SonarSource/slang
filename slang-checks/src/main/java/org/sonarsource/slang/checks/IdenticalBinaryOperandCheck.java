/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
package org.sonarsource.slang.checks;

import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;

import static org.sonarsource.slang.checks.utils.ExpressionUtils.containsPlaceHolder;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.skipParentheses;
import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S1764")
public class IdenticalBinaryOperandCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (tree.operator() != BinaryExpressionTree.Operator.PLUS
        && tree.operator() != BinaryExpressionTree.Operator.TIMES
        && !containsPlaceHolder(tree)
        && areEquivalent(skipParentheses(tree.leftOperand()), skipParentheses(tree.rightOperand()))) {
        ctx.reportIssue(
          tree.rightOperand(),
          "Correct one of the identical sub-expressions on both sides this operator",
          new SecondaryLocation(tree.leftOperand()));
      }
    });
  }

}
