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
package org.sonarsource.ruby.plugin;

import java.util.Arrays;
import java.util.List;
import org.sonarsource.ruby.checks.UnusedFunctionParameterRubyCheck;
import org.sonarsource.ruby.checks.UnusedLocalVariableRubyCheck;
import org.sonarsource.slang.checks.BooleanLiteralCheck;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.UnusedFunctionParameterCheck;
import org.sonarsource.slang.checks.UnusedLocalVariableCheck;
import org.sonarsource.slang.checks.UnusedPrivateMethodCheck;

public final class RubyCheckList {

  private RubyCheckList() {
    // utility class
  }

  static final Class[] RUBY_CHECK_BLACK_LIST = {
    BooleanLiteralCheck.class,
    UnusedPrivateMethodCheck.class,
    // Language specific implementation is provided.
    UnusedFunctionParameterCheck.class,
    UnusedLocalVariableCheck.class
  };

  static final List<Class<?>> RUBY_SPECIFIC_CHECKS = Arrays.asList(
    UnusedFunctionParameterRubyCheck.class,
    UnusedLocalVariableRubyCheck.class
  );

  public static List<Class<?>> checks() {
    List<Class<?>> list = CheckList.excludeChecks(RUBY_CHECK_BLACK_LIST);
    list.addAll(RUBY_SPECIFIC_CHECKS);
    return list;
  }

}
