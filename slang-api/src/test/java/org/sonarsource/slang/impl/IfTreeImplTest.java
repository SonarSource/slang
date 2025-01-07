/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
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

import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IfTreeImplTest {

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree condition = new LiteralTreeImpl(meta, "42");
    Tree thenBranch = new IdentifierTreeImpl(meta, "x");
    Tree elseBranch = new IdentifierTreeImpl(meta, "y");
    TokenImpl ifToken = new TokenImpl(new TextRangeImpl(1, 0, 1, 2), "if", Token.Type.KEYWORD);
    TokenImpl elseToken = new TokenImpl(new TextRangeImpl(2, 0, 1, 4), "else", Token.Type.KEYWORD);
    IfTreeImpl tree = new IfTreeImpl(meta, condition, thenBranch, elseBranch, ifToken, elseToken);
    assertThat(tree.children()).containsExactly(condition, thenBranch, elseBranch);
    assertThat(tree.condition()).isEqualTo(condition);
    assertThat(tree.thenBranch()).isEqualTo(thenBranch);
    assertThat(tree.elseBranch()).isEqualTo(elseBranch);

    assertThat(new IfTreeImpl(meta, condition, thenBranch, null, ifToken, null)
      .children()).containsExactly(condition, thenBranch);
  }

}
