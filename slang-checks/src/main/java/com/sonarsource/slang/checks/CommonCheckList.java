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

import java.util.Arrays;
import java.util.List;

public class CommonCheckList {

  private CommonCheckList() {
  }

  public static List<Class> checks() {
    return Arrays.asList(
      AllBranchesIdenticalCheck.class,
      BadClassNameCheck.class,
      BadFunctionNameCheck.class,
      BooleanLiteralCheck.class,
      CollapsibleIfStatementsCheck.class,
      DuplicateBranchCheck.class,
      DuplicatedFunctionImplementationCheck.class,
      ElseIfWithoutElseCheck.class,
      EmptyBlockCheck.class,
      EmptyCommentCheck.class,
      EmptyFunctionCheck.class,
      IfConditionalAlwaysTrueOrFalseCheck.class,
      FileHeaderCheck.class,
      FixMeCommentCheck.class,
      HardcodedCredentialsCheck.class,
      HardcodedIpCheck.class,
      IdenticalBinaryOperandCheck.class,
      IdenticalConditionsCheck.class,
      MatchCaseTooBigCheck.class,
      NestedMatchCheck.class,
      OneStatementPerLineCheck.class,
      SelfAssignmentCheck.class,
      StringLiteralDuplicatedCheck.class,
      TodoCommentCheck.class,
      TooLongFunctionCheck.class,
      TooLongLineCheck.class,
      TooManyLinesOfCodeFileCheck.class,
      TooManyCasesCheck.class,
      TooManyParametersCheck.class,
      UnusedFunctionParameterCheck.class,
      UnusedLocalVariableCheck.class,
      VariableAndParameterNameCheck.class);
  }

}
