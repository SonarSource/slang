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
package org.sonarsource.slang.impl;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionInvocationTreeImplTest {

  @Test
  void simple_function_invocation() {
    TreeMetaData meta = null;
    Tree identifierTree = new IdentifierTreeImpl(meta, "x");
    List<Tree> args = new ArrayList<>();

    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, identifierTree, args);
    assertThat(tree.children()).containsExactly(identifierTree);
    assertThat(tree.arguments()).isNotNull();
    assertThat(tree.arguments()).isEmpty();
    assertThat(tree.memberSelect()).isEqualTo(identifierTree);
  }

  @Test
  void function_invocation_with_arguments() {
    TreeMetaData meta = null;
    Tree identifierTree = new IdentifierTreeImpl(meta, "x");
    Tree arg1 = new IdentifierTreeImpl(meta, "x");
    Tree arg2 = new LiteralTreeImpl(meta, "x");
    List<Tree> args = new ArrayList<>();
    args.add(arg1);
    args.add(arg2);

    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, identifierTree, args);
    assertThat(tree.children()).containsExactly(identifierTree, arg1, arg2);
    assertThat(tree.arguments()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);
    assertThat(tree.arguments().get(0)).isEqualTo(arg1);
    assertThat(tree.arguments().get(1)).isEqualTo(arg2);
    assertThat(tree.memberSelect()).isEqualTo(identifierTree);
  }

  @Test
  void function_invocation_with_member_select() {
    TreeMetaData meta = null;
    IdentifierTree identifierTree = new IdentifierTreeImpl(meta, "y");
    Tree member = new IdentifierTreeImpl(meta, "x");
    Tree memberSelect = new MemberSelectTreeImpl(meta, member, identifierTree);
    List<Tree> args = new ArrayList<>();

    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, memberSelect, args);
    assertThat(tree.children()).containsExactly(memberSelect);
    assertThat(tree.memberSelect()).isEqualTo(memberSelect);
  }
}
