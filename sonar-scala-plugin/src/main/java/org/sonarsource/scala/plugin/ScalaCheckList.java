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
package org.sonarsource.scala.plugin;

import java.util.List;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.MatchWithoutElseCheck;
import org.sonarsource.slang.checks.OctalValuesCheck;
import org.sonarsource.slang.checks.RedundantParenthesesCheck;
import org.sonarsource.slang.checks.WrongAssignmentOperatorCheck;

public final class ScalaCheckList {

  private ScalaCheckList() {
    // utility class
  }

  private static final Class[] SCALA_CHECK_BLACK_LIST = {
    MatchWithoutElseCheck.class,
    OctalValuesCheck.class,
    RedundantParenthesesCheck.class,
    WrongAssignmentOperatorCheck.class
  };

  public static List<Class> checks() {
    return CheckList.excludeChecks(SCALA_CHECK_BLACK_LIST);
  }

}
