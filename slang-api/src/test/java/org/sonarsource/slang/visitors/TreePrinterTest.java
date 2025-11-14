/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.visitors;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.ModifierTreeImpl;
import org.sonarsource.slang.impl.TextRangeImpl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sonarsource.slang.utils.TreeCreationUtils.assignment;
import static org.sonarsource.slang.utils.TreeCreationUtils.binary;
import static org.sonarsource.slang.utils.TreeCreationUtils.identifier;
import static org.sonarsource.slang.utils.TreeCreationUtils.integerLiteral;

class TreePrinterTest {

  @Test
  void test() {
    Tree x1 = new IdentifierTreeImpl(null, "x1");
    Tree var1 = new IdentifierTreeImpl(null, "var1");
    Tree literal1 = new LiteralTreeImpl(null, "42");
    Tree binaryExp = new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.PLUS, null, var1, literal1);
    Tree assignExp = new AssignmentExpressionTreeImpl(null, AssignmentExpressionTree.Operator.EQUAL, x1, binaryExp);
    Tree modifier = new ModifierTreeImpl(null, ModifierTree.Kind.PRIVATE);
    Tree function = new FunctionDeclarationTreeImpl(null, singletonList(modifier), false, null, null, emptyList(), null, emptyList());
    Assertions.assertThat(TreePrinter.tree2string(Arrays.asList(assignExp, function))).isEqualTo("""
      AssignmentExpressionTreeImpl EQUAL
        IdentifierTreeImpl x1
        BinaryExpressionTreeImpl PLUS
          IdentifierTreeImpl var1
          LiteralTreeImpl 42

      FunctionDeclarationTreeImpl
        ModifierTreeImpl PRIVATE
      """);
  }

  @Test
  void table_test() {
    // xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = x-1;
    Tree add = binary(BinaryExpressionTree.Operator.PLUS,
        identifier("x", new TextRangeImpl(1,42,1,43),"x"),
        integerLiteral("1", new TextRangeImpl(1,44,1,45), "1"),
        new TextRangeImpl(1,42,1,45), "x", "1");

    Tree assign = assignment(identifier("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
        new TextRangeImpl(1,8,1,39), "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
        add,
        new TextRangeImpl(1,8,1,45),"x", "=", "x", "1");

    String actual = TreePrinter.table(assign);
    TreePrinter.Table expected = new TreePrinter.Table("AST node class", "first…last tokens", "line:col");
    expected.add("AssignmentExpressionTree {","x … 1","1:9 … 1:46");
    expected.add("  IdentifierTree","xxxxxxxxxxx…xxxxxxxxxxx","1:9 … 1:40");
    expected.add("  BinaryExpressionTree {","x … 1","1:43 … 1:46");
    expected.add("    IdentifierTree","x","1:43 … 1:44");
    expected.add("    IntegerLiteralTree","1","1:45 … 1:46");
    expected.add("  }","","");
    expected.add("}","","");
    assertEquals(expected.toString(), actual);
  }
}
