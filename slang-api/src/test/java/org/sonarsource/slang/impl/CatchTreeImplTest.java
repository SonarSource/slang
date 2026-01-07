/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource Sàrl
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

import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.junit.jupiter.api.Test;

import static org.sonarsource.slang.impl.TextRanges.range;
import static org.assertj.core.api.Assertions.assertThat;

class CatchTreeImplTest {

  @Test
  void test() {
    TreeMetaData meta = null;
    Token keyword = new TokenImpl(range(1, 2, 3, 4), "catch", Token.Type.KEYWORD);
    ParameterTree parameter = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta,"e"), null);
    Tree lhs = new IdentifierTreeImpl(meta, "x");
    Tree one = new LiteralTreeImpl(meta, "1");
    Tree assignmentExpressionTree =
        new AssignmentExpressionTreeImpl(meta, AssignmentExpressionTree.Operator.EQUAL, lhs, one);
    CatchTreeImpl catchWithIdentifier = new CatchTreeImpl(meta, parameter, assignmentExpressionTree, keyword);
    CatchTreeImpl catchWithoutIdentifier = new CatchTreeImpl(meta, null, assignmentExpressionTree, keyword);

    assertThat(catchWithIdentifier.children()).containsExactly(parameter, assignmentExpressionTree);
    assertThat(catchWithIdentifier.catchParameter()).isEqualTo(parameter);
    assertThat(catchWithIdentifier.catchBlock()).isEqualTo(assignmentExpressionTree);
    assertThat(catchWithIdentifier.keyword()).isEqualTo(keyword);

    assertThat(catchWithoutIdentifier.children()).containsExactly(assignmentExpressionTree);
    assertThat(catchWithoutIdentifier.catchParameter()).isNull();
    assertThat(catchWithoutIdentifier.catchBlock()).isEqualTo(assignmentExpressionTree);
  }
}
