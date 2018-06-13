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
package com.sonarsource.slang.api;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static com.sonarsource.slang.TreeCreationUtils.assignment;
import static com.sonarsource.slang.TreeCreationUtils.binary;
import static com.sonarsource.slang.TreeCreationUtils.block;
import static com.sonarsource.slang.TreeCreationUtils.identifier;
import static com.sonarsource.slang.TreeCreationUtils.literal;
import static com.sonarsource.slang.TreeCreationUtils.simpleFunction;
import static com.sonarsource.slang.TreeCreationUtils.topLevel;
import static com.sonarsource.slang.api.BinaryExpressionTree.Operator.EQUAL_TO;
import static org.assertj.core.api.Assertions.assertThat;

public class TreeTest {

  @Test
  public void test() {
    Tree x = identifier("x");
    Tree y = identifier("y");
    Tree int1 = literal("1");
    Tree xEqualTo1 = binary(EQUAL_TO, x, int1);
    Tree yEqualsXEqualTo1 = assignment(y, xEqualTo1);
    IdentifierTree functionName = identifier("x");
    BlockTree functionBody = block(Arrays.asList(xEqualTo1, yEqualsXEqualTo1));
    Tree function = simpleFunction(functionName, functionBody);
    Tree topLevelTree = topLevel(Collections.singletonList(function));

    assertThat(topLevelTree.descendants())
      .containsExactly(function, functionName, functionBody, xEqualTo1, x, int1, yEqualsXEqualTo1, y, xEqualTo1, x, int1);
    assertThat(function.descendants())
      .containsExactly(functionName, functionBody, xEqualTo1, x, int1, yEqualsXEqualTo1, y, xEqualTo1, x, int1);
    assertThat(yEqualsXEqualTo1.descendants())
      .containsExactly(y, xEqualTo1, x, int1);
  }

}
