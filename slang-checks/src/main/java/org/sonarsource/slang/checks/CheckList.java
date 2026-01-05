/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CheckList {

  // these checks should be explicitly added to 'checks' in Sensor constructor
  // and language RulesDefinition
  // this list should be maintained only for documentation purposes (keep the track of all existing checks in this class) and for testing
  static final Class[] ALL_CHECKS_WITH_LANGUAGE_CONFIG = {
    CommentedCodeCheck.class,
  };

  private CheckList() {
  }

  static List<Class<?>> allChecks() {
    return Arrays.asList(
      AllBranchesIdenticalCheck.class,
      BadClassNameCheck.class,
      BadFunctionNameCheck.class,
      BooleanInversionCheck.class,
      BooleanLiteralCheck.class,
      CodeAfterJumpCheck.class,
      CollapsibleIfStatementsCheck.class,
      DuplicateBranchCheck.class,
      DuplicatedFunctionImplementationCheck.class,
      ElseIfWithoutElseCheck.class,
      EmptyBlockCheck.class,
      EmptyCommentCheck.class,
      EmptyFunctionCheck.class,
      FileHeaderCheck.class,
      FixMeCommentCheck.class,
      FunctionCognitiveComplexityCheck.class,
      HardcodedCredentialsCheck.class,
      HardcodedIpCheck.class,
      IdenticalBinaryOperandCheck.class,
      IdenticalConditionsCheck.class,
      IfConditionalAlwaysTrueOrFalseCheck.class,
      MatchCaseTooBigCheck.class,
      MatchWithoutElseCheck.class,
      NestedMatchCheck.class,
      OctalValuesCheck.class,
      OneStatementPerLineCheck.class,
      ParsingErrorCheck.class,
      RedundantParenthesesCheck.class,
      SelfAssignmentCheck.class,
      StringLiteralDuplicatedCheck.class,
      TabsCheck.class,
      TodoCommentCheck.class,
      TooComplexExpressionCheck.class,
      TooDeeplyNestedStatementsCheck.class,
      TooLongFunctionCheck.class,
      TooLongLineCheck.class,
      TooManyLinesOfCodeFileCheck.class,
      TooManyCasesCheck.class,
      TooManyParametersCheck.class,
      UnusedFunctionParameterCheck.class,
      UnusedLocalVariableCheck.class,
      UnusedPrivateMethodCheck.class,
      VariableAndParameterNameCheck.class,
      WrongAssignmentOperatorCheck.class);
  }

  public static List<Class<?>> excludeChecks(Class[] blackList) {
    List<Class<?>> checks = new ArrayList<>(allChecks());
    checks.removeAll(Arrays.asList(blackList));
    return checks;
  }

}
