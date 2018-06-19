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

import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.checks.utils.ExpressionUtils;
import java.util.function.Predicate;
import org.sonar.check.Rule;

import static com.sonarsource.slang.api.BinaryExpressionTree.Operator.CONDITIONAL_AND;
import static com.sonarsource.slang.api.BinaryExpressionTree.Operator.CONDITIONAL_OR;
import static com.sonarsource.slang.checks.utils.ExpressionUtils.isBinaryOperation;
import static com.sonarsource.slang.checks.utils.ExpressionUtils.isBooleanLiteral;
import static com.sonarsource.slang.checks.utils.ExpressionUtils.isFalseValueLiteral;
import static com.sonarsource.slang.checks.utils.ExpressionUtils.isNegation;
import static com.sonarsource.slang.checks.utils.ExpressionUtils.isTrueValueLiteral;

@Rule(key = "S1145")
public class IfConditionalAlwaysTrueOrFalseCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, ifTree) -> {
      Tree condition = ifTree.condition();
      if (isAlwaysTrueOrFalse(condition)) {
        ctx.reportIssue(condition, "Remove this useless \"if\" statement.");
      }
    });
  }

  private static boolean isAlwaysTrueOrFalse(Tree condition) {
    return isBooleanLiteral(condition)
      || isTrueValueLiteral(condition)
      || isFalseValueLiteral(condition)
      || isSimpleExpressionWithLiteral(condition, CONDITIONAL_AND, ExpressionUtils::isFalseValueLiteral)
      || isSimpleExpressionWithLiteral(condition, CONDITIONAL_OR, ExpressionUtils::isTrueValueLiteral);
  }

  private static boolean isSimpleExpressionWithLiteral(Tree condition, Operator operator, Predicate<? super Tree> hasLiteralValue) {
    boolean simpleExpression = isBinaryOperation(condition, operator)
      && condition.descendants()
        .allMatch(tree -> tree instanceof IdentifierTree
          || tree instanceof LiteralTree
          || isNegation(tree)
          || isBinaryOperation(tree, operator));

    return simpleExpression && condition.descendants().anyMatch(hasLiteralValue);
  }

}
