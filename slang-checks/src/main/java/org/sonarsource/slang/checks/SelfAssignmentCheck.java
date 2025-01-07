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
package org.sonarsource.slang.checks;

import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S1656")
public class SelfAssignmentCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(AssignmentExpressionTree.class, (ctx, tree) -> {
      if (tree.operator() == AssignmentExpressionTree.Operator.EQUAL && areEquivalent(tree.leftHandSide(), tree.statementOrExpression())) {
        ctx.reportIssue(tree, "Remove or correct this useless self-assignment.");
      }
    });
  }

}
