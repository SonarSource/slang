/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
package org.sonar.go.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.sonarsource.slang.checks.BadClassNameCheck;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.CodeAfterJumpCheck;
import org.sonarsource.slang.checks.CollapsibleIfStatementsCheck;
import org.sonarsource.slang.checks.OneStatementPerLineCheck;
import org.sonarsource.slang.checks.TabsCheck;
import org.sonarsource.slang.checks.UnusedFunctionParameterCheck;
import org.sonarsource.slang.checks.UnusedLocalVariableCheck;
import org.sonarsource.slang.checks.UnusedPrivateMethodCheck;

import org.sonar.go.checks.CodeAfterJumpGoCheck;
import org.sonar.go.checks.OneStatementPerLineGoCheck;

public class GoCheckList {

  private GoCheckList() {
    // utility class
  }

  static final Class[] GO_CHECK_BLACK_LIST = {
    BadClassNameCheck.class,
    // Can not enable rule S1066, as Go if-trees are containing an initializer, not well handled by SLang
    CollapsibleIfStatementsCheck.class,
    TabsCheck.class,
    // Can not enable rule S1172 since it it not possible to identify overridden function with modifier (to avoid FP)
    UnusedFunctionParameterCheck.class,
    UnusedLocalVariableCheck.class,
    UnusedPrivateMethodCheck.class,
    // Replaced by language specific test
    CodeAfterJumpCheck.class,
    OneStatementPerLineCheck.class
  };

  private static final Collection<Class<?>> GO_LANGUAGE_SPECIFIC_CHECKS = Arrays.asList(
    CodeAfterJumpGoCheck.class,
    OneStatementPerLineGoCheck.class
  );

  public static List<Class<?>> checks() {
    List<Class<?>> list = new ArrayList<>(CheckList.excludeChecks(GO_CHECK_BLACK_LIST));
    list.addAll(GO_LANGUAGE_SPECIFIC_CHECKS);
    return list;
  }
}
