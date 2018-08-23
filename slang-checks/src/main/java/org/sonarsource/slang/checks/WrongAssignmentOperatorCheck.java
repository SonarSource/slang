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
package org.sonarsource.slang.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.impl.TextRanges;

import static java.util.Arrays.asList;
import static org.sonarsource.slang.api.AssignmentExpressionTree.Operator.EQUAL;


@Rule(key = "S2757")
public class WrongAssignmentOperatorCheck implements SlangCheck {

  private static final List<String> SUSPICIOUS_TOKEN_VALUES = asList("!", "+", "-");

  @Override
  public void initialize(InitContext init) {
    init.register(AssignmentExpressionTree.class, (ctx, assignment) -> {
      if (assignment.operator() != EQUAL) {
        return;
      }

      List<Token> leftHandSideTokens = assignment.leftHandSide().metaData().tokens();
      Token variableLastToken = leftHandSideTokens.get(leftHandSideTokens.size() - 1);

      List<Token> allTokens = assignment.metaData().tokens();
      Token operatorToken = allTokens.get(allTokens.indexOf(variableLastToken) + 1);

      Token expressionFirstToken = assignment.statementOrExpression().metaData().tokens().get(0);

      if (isSuspiciousToken(expressionFirstToken)
        && noSpacingBetween(operatorToken, expressionFirstToken)
        && !noSpacingBetween(variableLastToken, operatorToken)) {
        TextRange range = TextRanges.merge(asList(operatorToken.textRange(), expressionFirstToken.textRange()));
        ctx.reportIssue(range, getMessage(expressionFirstToken, ctx));
      }
    });
  }

  private static String getMessage(Token expressionFirstToken, CheckContext aeTree) {
    if (isSingleNegationAssignment(expressionFirstToken, aeTree)) {
      // For expressions such as "a = b =! c" we want to display the other message
      return "Add a space between \"=\" and \"!\" to avoid confusion.";
    }
    return "Was \"" + expressionFirstToken.text() + "=\" meant instead?";
  }

  private static boolean isSingleNegationAssignment(Token firstToken, CheckContext aeTree) {
    return "!".equals(firstToken.text()) && !(aeTree.parent() instanceof AssignmentExpressionTree);
  }

  private static boolean noSpacingBetween(Token firstToken, Token secondToken) {
    return firstToken.textRange().end().line() == secondToken.textRange().start().line()
      && firstToken.textRange().end().lineOffset() == secondToken.textRange().start().lineOffset();
  }

  private static boolean isSuspiciousToken(Token firstToken) {
    return SUSPICIOUS_TOKEN_VALUES.contains(firstToken.text());
  }

}
