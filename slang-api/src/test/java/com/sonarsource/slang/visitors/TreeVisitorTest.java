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
package com.sonarsource.slang.visitors;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.NativeKind;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonarsource.slang.impl.NativeTreeImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TreeVisitorTest {

  private class DummyNativeKind implements NativeKind {}

  private IdentifierTree var1 = new IdentifierTreeImpl(null, "var1");
  private LiteralTree number1 = new LiteralTreeImpl(null, "1");
  private BinaryExpressionTree binary = new BinaryExpressionTreeImpl(null, Operator.PLUS, var1, number1);
  private BinaryExpressionTree binminus = new BinaryExpressionTreeImpl(null, Operator.MINUS, var1, var1);

  private DummyNativeKind nkind = new DummyNativeKind();
  private NativeTree nativeNode = new NativeTreeImpl(null, nkind, Arrays.asList(binary, binminus));
  
  private TreeVisitor<TreeContext> visitor = new TreeVisitor<>();

  @Test
  public void visitSimpleTree() {
    List<Tree> visited = new ArrayList<>();
    visitor.register(Tree.class, (ctx, tree) -> visited.add(tree));
    visitor.scan(new TreeContext(), binary);
    assertThat(visited).containsExactly(binary, var1, number1);
  }

  @Test
  public void visitNativeTree() {
    List<Tree> visited = new ArrayList<>();
    visitor.register(Tree.class, (ctx, tree) -> visited.add(tree));
    visitor.scan(new TreeContext(), nativeNode);
    assertThat(visited).containsExactly(nativeNode, binary, var1, number1, binminus, var1, var1);
  }

  @Test
  public void ancestors() {
    Map<Tree, List<Tree>> ancestors = new HashMap<>();
    visitor.register(Tree.class, (ctx, tree) -> ancestors.put(tree, new ArrayList<Tree>(ctx.ancestors())));
    visitor.scan(new TreeContext(), binary);
    assertThat(ancestors.get(binary)).isEmpty();
    assertThat(ancestors.get(var1)).containsExactly(binary);
    assertThat(ancestors.get(number1)).containsExactly(binary);
  }
}
