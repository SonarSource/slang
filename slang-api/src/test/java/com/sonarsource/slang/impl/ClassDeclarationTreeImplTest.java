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

import com.sonarsource.slang.api.ClassDeclarationTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.NativeKind;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

public class ClassDeclarationTreeImplTest {

  private class ClassNativeKind implements NativeKind {}

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void test() {
    TreeMetaData meta = null;
    IdentifierTree className = new IdentifierTreeImpl(meta, "MyClass");
    Tree classDecl = new NativeTreeImpl(meta, new ClassNativeKind(), Collections.singletonList(className));
    ClassDeclarationTree tree = new ClassDeclarationTreeImpl(meta, className, classDecl);
    assertThat(tree.children()).hasSize(1);
    assertThat(areEquivalent(tree.children().get(0), classDecl)).isTrue();
    assertThat(areEquivalent(tree.identifier(), className)).isTrue();
  }

  @Test
  public void test_failure() {
    IdentifierTree className = new IdentifierTreeImpl(null, "MyClass");
    Tree classDecl = new NativeTreeImpl(null, new ClassNativeKind(), Collections.emptyList());
    expected.expect(IllegalStateException.class);
    expected.expectMessage("Class identifier must be inside the native classTree structure");
    new ClassDeclarationTreeImpl(null, className, classDecl);
  }

}
