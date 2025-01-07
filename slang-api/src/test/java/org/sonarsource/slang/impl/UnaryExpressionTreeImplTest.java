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

import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

class UnaryExpressionTreeImplTest  {

  private class TypeNativeKind implements NativeKind {}

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree condition = new IdentifierTreeImpl(meta, "x");
    Tree negCondition = new UnaryExpressionTreeImpl(meta, UnaryExpressionTree.Operator.NEGATE, condition);
    Tree negConditionCopy = new UnaryExpressionTreeImpl(meta, UnaryExpressionTree.Operator.NEGATE, condition);
    Tree nativeTree = new NativeTreeImpl(meta, new TypeNativeKind(), Arrays.asList(condition));
    Tree negNative = new UnaryExpressionTreeImpl(meta, UnaryExpressionTree.Operator.NEGATE, nativeTree);

    assertThat(negCondition.children()).containsExactly(condition);
    assertThat(areEquivalent(negCondition, negConditionCopy)).isTrue();
    assertThat(areEquivalent(negNative, negCondition)).isFalse();
  }

}
