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
package org.sonarsource.slang.impl;

import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.junit.Test;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

public class ParameterTreeImplTest {

  private class TypeNativeKind implements NativeKind {}

  @Test
  public void test() {
    TreeMetaData meta = null;
    Tree parameterType = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    IdentifierTree identifierTreeX = new IdentifierTreeImpl(meta, "x");
    IdentifierTree identifierTreeY = new IdentifierTreeImpl(meta, "y");
    ParameterTreeImpl parameterTreeX = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeXCopy = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta, "x"), null);
    ParameterTreeImpl parameterTreeXTyped = new ParameterTreeImpl(meta, identifierTreeX, parameterType);
    ParameterTreeImpl parameterTreeY = new ParameterTreeImpl(meta, identifierTreeY, parameterType);

    assertThat(parameterTreeXTyped.children()).hasSize(2);
    assertThat(parameterTreeX.children()).hasSize(1);
    assertThat(parameterTreeX.type()).isNull();
    assertThat(parameterTreeX.identifier()).isEqualTo(identifierTreeX);
    assertThat(areEquivalent(parameterTreeX, parameterTreeXCopy)).isTrue();
    assertThat(areEquivalent(parameterTreeX, parameterTreeXTyped)).isFalse();
    assertThat(areEquivalent(parameterTreeX, parameterTreeY)).isFalse();
    assertThat(areEquivalent(parameterTreeXTyped, parameterTreeY)).isFalse();
  }

}
