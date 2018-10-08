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

  private static final Class[] RUBY_CHECK_BLACK_LIST = {
    BooleanLiteralCheck.class,
    UnusedPrivateMethodCheck.class,
  };

  private static final Class[] KOTLIN_CHECK_BLACK_LIST = {
    // FP rate too high for now in Kotlin on 'when' statements due to enum/sealed class that have all branches covered
    MatchWithoutElseCheck.class,
    // Rule does not apply here as octal values do not exist in Kotlin
    OctalValuesCheck.class
  };

  private static final Class[] SCALA_CHECK_BLACK_LIST = {
    BooleanLiteralCheck.class,
    CodeAfterJumpCheck.class,
    DuplicateBranchCheck.class,
    DuplicatedFunctionImplementationCheck.class,
    FunctionCognitiveComplexityCheck.class,
    HardcodedCredentialsCheck.class,
    IdenticalConditionsCheck.class,
    MatchWithoutElseCheck.class,
    OctalValuesCheck.class,
    RedundantParenthesesCheck.class,
    TooComplexExpressionCheck.class,
    UnusedFunctionParameterCheck.class,
    UnusedPrivateMethodCheck.class,
    VariableAndParameterNameCheck.class,
    WrongAssignmentOperatorCheck.class
  };

  private CheckList() {
  }

  static List<Class> allChecks() {
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

  public static List<Class> kotlinChecks() {
    return excludeChecks(KOTLIN_CHECK_BLACK_LIST);
  }

  public static List<Class> rubyChecks() {
    return excludeChecks(RUBY_CHECK_BLACK_LIST);
  }

  public static List<Class> scalaChecks() {
    return excludeChecks(SCALA_CHECK_BLACK_LIST);
  }

  private static List<Class> excludeChecks(Class[] blackList) {
    List<Class> checks = new ArrayList<>(allChecks());
    checks.removeAll(Arrays.asList(blackList));
    return checks;
  }

}
