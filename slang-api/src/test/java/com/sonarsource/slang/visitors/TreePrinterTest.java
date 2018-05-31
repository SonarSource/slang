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

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TreePrinterTest {

  @Test
  public void simple_binary() { ;
    Tree var1 = new IdentifierTreeImpl(null, "var1");
    Tree literal1 = new LiteralTreeImpl(null, "42");
    BinaryExpressionTreeImpl binaryExp = new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.PLUS, var1, literal1);
    Assertions.assertThat(TreePrinter.tree2string(binaryExp)).isEqualTo(
      "BinaryExpressionTreeImpl PLUS\n" +
        "  IdentifierTreeImpl var1\n" +
        "  LiteralTreeImpl 42\n");
  }
}
