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
package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.TreeMetaData;
import org.junit.Test;

import static com.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

public class ReturnTreeImplTest {
  @Test
  public void test() {
    TreeMetaData meta = null;
    TokenImpl returnKeyword = new TokenImpl(new TextRangeImpl(1, 0, 1, 6), "return", Token.Type.KEYWORD);
    ReturnTreeImpl returnWithoutValue = new ReturnTreeImpl(meta, returnKeyword, null);

    assertThat(returnWithoutValue.children()).hasSize(0);
    assertThat(returnWithoutValue.keyword().text()).isEqualTo("return");
    assertThat(returnWithoutValue.body()).isNull();

    ReturnTreeImpl returnWithValue = new ReturnTreeImpl(meta, returnKeyword, new LiteralTreeImpl(meta, "foo"));
    assertThat(returnWithValue.children()).hasSize(1);
    assertThat(returnWithValue.keyword().text()).isEqualTo("return");
    assertThat(returnWithValue.body()).isInstanceOf(LiteralTree.class);

    assertThat(areEquivalent(returnWithoutValue, new ReturnTreeImpl(meta, returnKeyword, null))).isTrue();
    assertThat(areEquivalent(returnWithoutValue, returnWithValue)).isFalse();

  }
}
