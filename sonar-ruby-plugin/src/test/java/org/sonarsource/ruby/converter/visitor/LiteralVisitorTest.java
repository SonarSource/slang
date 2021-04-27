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
package org.sonarsource.ruby.converter.visitor;


import org.junit.jupiter.api.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.IntegerLiteralTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.api.IntegerLiteralTree.Base.BINARY;
import static org.sonarsource.slang.api.IntegerLiteralTree.Base.DECIMAL;
import static org.sonarsource.slang.api.IntegerLiteralTree.Base.HEXADECIMAL;
import static org.sonarsource.slang.api.IntegerLiteralTree.Base.OCTAL;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.sonarsource.slang.testing.TreesAssert.assertTrees;

class LiteralVisitorTest extends AbstractRubyConverterTest {

  @Test
  void boolean_literal() {
    assertTree(rubyStatement("true")).isLiteral("true");
    assertTree(rubyStatement("false")).isLiteral("false");
  }

  @Test
  void int_literals() {
    assertTrees(rubyStatements("2; 512; 4\n2431323"))
      .isEquivalentTo(slangStatements("2; 512; 4; 2431323;"));
    assertTree(rubyStatement("2")).isLiteral("2");

    // literal bigger than Long.MAX_VALUE are returned as BigInteger by JRuby
    assertTree(rubyStatement("10000000000000000000")).isLiteral("10000000000000000000");
  }

  @Test
  void octal_int_literals() {
    IntegerLiteralTree literal0 = (IntegerLiteralTree) rubyStatement("0252");
    assertTree(literal0).isLiteral("0252");
    assertThat(literal0.getBase()).isEqualTo(OCTAL);
    assertThat(literal0.getIntegerValue().intValue()).isEqualTo(170);

    IntegerLiteralTree literal1 = (IntegerLiteralTree) rubyStatement("0o252");
    assertTree(literal1).isLiteral("0o252");
    assertThat(literal1.getBase()).isEqualTo(OCTAL);
    assertThat(literal1.getIntegerValue().intValue()).isEqualTo(170);

    IntegerLiteralTree literal2 = (IntegerLiteralTree) rubyStatement("0O252");
    assertTree(literal2).isLiteral("0O252");
    assertThat(literal2.getBase()).isEqualTo(OCTAL);
    assertThat(literal2.getIntegerValue().intValue()).isEqualTo(170);
  }

  @Test
  void other_int_literals() {
    IntegerLiteralTree literal0 = (IntegerLiteralTree) rubyStatement("0xaa");
    assertTree(literal0).isLiteral("0xaa");
    assertThat(literal0.getBase()).isEqualTo(HEXADECIMAL);
    assertThat(literal0.getIntegerValue().intValue()).isEqualTo(170);
    assertThat(literal0.getNumericPart()).isEqualTo("aa");

    IntegerLiteralTree literal1 = (IntegerLiteralTree) rubyStatement("0D123");
    assertTree(literal1).isLiteral("0D123");
    assertThat(literal1.getBase()).isEqualTo(DECIMAL);
    assertThat(literal1.getIntegerValue().intValue()).isEqualTo(123);
    assertThat(literal1.getNumericPart()).isEqualTo("123");

    IntegerLiteralTree literal2 = (IntegerLiteralTree) rubyStatement("123");
    assertTree(literal2).isLiteral("123");
    assertThat(literal2.getBase()).isEqualTo(DECIMAL);
    assertThat(literal2.getIntegerValue().intValue()).isEqualTo(123);
    assertThat(literal2.getNumericPart()).isEqualTo("123");

    IntegerLiteralTree literal3 = (IntegerLiteralTree) rubyStatement("0b101");
    assertTree(literal3).isLiteral("0b101");
    assertThat(literal3.getBase()).isEqualTo(BINARY);
    assertThat(literal3.getIntegerValue().intValue()).isEqualTo(5);
    assertThat(literal3.getNumericPart()).isEqualTo("101");
  }

}
