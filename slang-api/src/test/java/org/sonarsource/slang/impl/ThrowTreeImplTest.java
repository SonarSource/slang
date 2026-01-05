/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.impl;

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;

class ThrowTreeImplTest {
  @Test
  void test() {
    TreeMetaData meta = null;
    TokenImpl throwKeyword = new TokenImpl(new TextRangeImpl(1, 0, 1, 6), "throw", Token.Type.KEYWORD);
    ThrowTreeImpl throwWithoutValue = new ThrowTreeImpl(meta, throwKeyword, null);

    assertThat(throwWithoutValue.children()).isEmpty();
    assertThat(throwWithoutValue.keyword().text()).isEqualTo("throw");
    assertThat(throwWithoutValue.body()).isNull();

    ThrowTreeImpl throwWithValue = new ThrowTreeImpl(meta, throwKeyword, new LiteralTreeImpl(meta, "foo"));
    assertThat(throwWithValue.children()).hasSize(1);
    assertThat(throwWithValue.keyword().text()).isEqualTo("throw");
    assertThat(throwWithValue.body()).isInstanceOf(LiteralTree.class);

    assertThat(areEquivalent(throwWithoutValue, new ThrowTreeImpl(meta, throwKeyword, null))).isTrue();
    assertThat(areEquivalent(throwWithoutValue, throwWithValue)).isFalse();

  }
}
