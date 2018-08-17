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
package org.sonarsource.ruby.converter.impl;

import org.junit.Test;
import org.sonarsource.slang.api.CatchTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.CatchTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class RubyPartialExceptionHandlingTreeTest {

  @Test
  public void test() {
    Tree tryBlock = new LiteralTreeImpl(null, "1");
    Tree catchBlock = new LiteralTreeImpl(null, "2");
    CatchTree catchTree = new CatchTreeImpl(null, null, catchBlock, null);

    RubyPartialExceptionHandlingTree tree1 = new RubyPartialExceptionHandlingTree(null, emptyList());
    assertThat(tree1.children()).isEmpty();
    assertThat(tree1.metaData()).isNull();

    tree1.setFinallyBlock(new IdentifierTreeImpl(null, "x"));
    assertThat(tree1.children()).hasSize(1);
    assertTree(tree1.children().get(0)).isIdentifier("x");

    RubyPartialExceptionHandlingTree tree2 = new RubyPartialExceptionHandlingTree(tryBlock, singletonList(catchTree));
    assertThat(tree2.children()).containsExactly(tryBlock, catchTree);
  }

}