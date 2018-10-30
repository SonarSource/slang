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
package org.sonarsource.slang.testing;

import org.junit.Test;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.TextRangeImpl;

import static org.junit.Assert.assertEquals;
import static org.sonarsource.slang.testing.TreeCreationUtils.assignment;
import static org.sonarsource.slang.testing.TreeCreationUtils.binary;
import static org.sonarsource.slang.testing.TreeCreationUtils.identifier;
import static org.sonarsource.slang.testing.TreeCreationUtils.integerLiteral;

public class UtilsTest {

  @Test
  public void table_test() {
    // x = x-1;
    Tree add = binary(BinaryExpressionTree.Operator.PLUS,
        identifier("x", new TextRangeImpl(1,12,1,13),"x"),
        integerLiteral("1", new TextRangeImpl(1,14,1,15), "1"),
        new TextRangeImpl(1,12,1,15), "x", "1");

    Tree assign = assignment(identifier("x", new TextRangeImpl(1,8,1,9), "x"), add,
        new TextRangeImpl(1,8,1,15),"x", "=", "x", "1");

    String actual = Utils.table(assign);
    Utils.Table expected = new Utils.Table("AST node class", "first…last tokens", "line:col");
    expected.add("AssignmentExpressionTree {","x … 1","1:9 … 1:16");
    expected.add("  IdentifierTree","x","1:9 … 1:10");
    expected.add("  BinaryExpressionTree {","x … 1","1:13 … 1:16");
    expected.add("    IdentifierTree","x","1:13 … 1:14");
    expected.add("    IntegerLiteralTree","1","1:15 … 1:16");
    expected.add("  }","","");
    expected.add("}","","");
    assertEquals(expected.toString(), actual);
  }
}
