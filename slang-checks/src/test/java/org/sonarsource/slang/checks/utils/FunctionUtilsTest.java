/*
 * SonarSource SLang
 * Copyright (C) 2018-2019 SonarSource SA
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
package org.sonarsource.slang.checks.utils;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.FunctionInvocationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.MemberSelectTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;

import static org.sonarsource.slang.checks.utils.FunctionUtils.hasFunctionCallName;
import static org.assertj.core.api.Assertions.assertThat;

public class FunctionUtilsTest {
  private class TypeNativeKind implements NativeKind {}

  private static TreeMetaData meta = null;
  private static IdentifierTree identifierTree = new IdentifierTreeImpl(meta, "function");
  private static List<Tree> args = new ArrayList<>();

  @Test
  public void test_has_function_name_identifier() {
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, identifierTree, args);
    assertThat(hasFunctionCallName(tree, "function")).isTrue();
    assertThat(hasFunctionCallName(tree, "FuNcTiOn")).isTrue();
    assertThat(hasFunctionCallName(tree, "mySuperFunction")).isFalse();
  }

  @Test
  public void test_has_function_name_method_select() {
    Tree member = new IdentifierTreeImpl(meta, "A");
    Tree methodSelect = new MemberSelectTreeImpl(meta, member, identifierTree);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, methodSelect, args);
    assertThat(hasFunctionCallName(tree, "function")).isTrue();
    assertThat(hasFunctionCallName(tree, "A")).isFalse();
  }

  @Test
  public void test_has_function_name_unknown() {
    Tree nativeNode = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, nativeNode, args);
    assertThat(hasFunctionCallName(tree, "function")).isFalse();
  }

}
