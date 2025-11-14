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
package org.sonarsource.slang.checks;

import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import java.util.EnumMap;
import java.util.Map;
import org.sonar.check.Rule;

import static org.sonarsource.slang.checks.utils.ExpressionUtils.skipParentheses;

@Rule(key = "S1940")
public class BooleanInversionCheck implements SlangCheck {

  private static final Map<Operator, String> OPERATORS = createOperatorsMap();

  private static Map<Operator, String> createOperatorsMap() {
    Map<Operator, String> operatorsMap = new EnumMap<>(Operator.class);
    operatorsMap.put(Operator.EQUAL_TO, "!=");
    operatorsMap.put(Operator.NOT_EQUAL_TO, "==");
    operatorsMap.put(Operator.LESS_THAN, ">=");
    operatorsMap.put(Operator.GREATER_THAN, "<=");
    operatorsMap.put(Operator.LESS_THAN_OR_EQUAL_TO, ">");
    operatorsMap.put(Operator.GREATER_THAN_OR_EQUAL_TO, "<");
    return operatorsMap;
  }

  @Override
  public void initialize(InitContext init) {
    init.register(UnaryExpressionTree.class, (ctx, tree) -> {
      Tree innerExpression = skipParentheses(tree.operand());
      if (tree.operator() == UnaryExpressionTree.Operator.NEGATE && innerExpression instanceof BinaryExpressionTree) {
        BinaryExpressionTree binaryExpression = (BinaryExpressionTree) innerExpression;
        String oppositeOperator = OPERATORS.get(binaryExpression.operator());
        if (oppositeOperator != null) {
          String message = String.format("Use the opposite operator (\"%s\") instead.", oppositeOperator);
          ctx.reportIssue(tree, message);
        }
      }
    });
  }

}
