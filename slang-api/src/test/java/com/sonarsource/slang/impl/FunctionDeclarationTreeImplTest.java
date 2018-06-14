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

import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.ParameterTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import java.util.List;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class FunctionDeclarationTreeImplTest {

  @Test
  public void test() {
    TreeMetaData meta = null;
    List<Tree> modifiers = singletonList(new IdentifierTreeImpl(meta, "public"));
    Tree returnType = new IdentifierTreeImpl(meta, "int");
    IdentifierTree name = new IdentifierTreeImpl(meta, "foo");
    IdentifierTree paramName = new IdentifierTreeImpl(meta, "p1");
    ParameterTree param = new ParameterTreeImpl(meta, paramName, null);
    List<ParameterTree> params = singletonList(param);
    BlockTree body = new BlockTreeImpl(meta, emptyList());

    FunctionDeclarationTreeImpl tree = new FunctionDeclarationTreeImpl(meta, modifiers, returnType, name, params, body);
    assertThat(tree.children()).hasSize(5);
    assertThat(tree.children()).containsExactly(modifiers.get(0), returnType, name, param, body);
    assertThat(tree.modifiers()).isEqualTo(modifiers);
    assertThat(tree.returnType()).isEqualTo(returnType);
    assertThat(tree.name()).isEqualTo(name);
    assertThat(tree.formalParameters()).isEqualTo(params);
    assertThat(tree.body()).isEqualTo(body);

    assertThat(new FunctionDeclarationTreeImpl(meta, modifiers, null, null, emptyList(), null).children())
      .containsExactly(modifiers.get(0));
  }
}
