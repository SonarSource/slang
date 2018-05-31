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

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;

import static com.sonarsource.slang.checks.utils.SyntacticEquivalence.areEquivalent;

public class IdenticalBinaryOperandCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (tree.operator() != BinaryExpressionTree.Operator.PLUS
        && tree.operator() != BinaryExpressionTree.Operator.TIMES
        && areEquivalent(tree.leftOperand(), tree.rightOperand())) {
        ctx.reportIssue(tree, "Correct one of the identical sub-expressions on both sides this operator");
      }
    });
  }

}
