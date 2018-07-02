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
package com.sonarsource.slang.visitors;

import com.sonarsource.slang.api.AssignmentExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.ModifierTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.ModifierTreeImpl;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class TreePrinterTest {

  @Test
  public void test() {
    Tree x1 = new IdentifierTreeImpl(null, "x1");
    Tree var1 = new IdentifierTreeImpl(null, "var1");
    Tree literal1 = new LiteralTreeImpl(null, "42");
    Tree binaryExp = new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.PLUS, null, var1, literal1);
    Tree assignExp = new AssignmentExpressionTreeImpl(null, AssignmentExpressionTree.Operator.EQUAL, x1, binaryExp);
    Tree modifier = new ModifierTreeImpl(null, ModifierTree.Kind.PRIVATE);
    Tree function = new FunctionDeclarationTreeImpl(null, singletonList(modifier), null, null, emptyList(), null, emptyList());
    Assertions.assertThat(TreePrinter.tree2string(Arrays.asList(assignExp, function))).isEqualTo(
      "AssignmentExpressionTreeImpl EQUAL\n" +
        "  IdentifierTreeImpl x1\n" +
        "  BinaryExpressionTreeImpl PLUS\n" +
        "    IdentifierTreeImpl var1\n" +
        "    LiteralTreeImpl 42\n" +
        "\n" +
        "FunctionDeclarationTreeImpl\n" +
        "  ModifierTreeImpl PRIVATE\n");
  }
}
