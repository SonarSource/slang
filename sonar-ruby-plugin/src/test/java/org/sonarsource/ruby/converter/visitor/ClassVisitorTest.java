/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.ruby.converter.visitor;


import org.junit.jupiter.api.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class ClassVisitorTest extends AbstractRubyConverterTest {

  @Test
  void simple_class() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class A\ndef foo()\nend\nend");
    assertTree(tree.identifier()).isIdentifier("A");
    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    assertThat(nativeClassTree).isInstanceOf(NativeTree.class);
    assertThat(nativeClassTree.nativeKind()).isEqualTo(nativeKind("class"));
    assertThat(nativeClassTree.children().get(0)).isEqualTo(tree.identifier());
  }

  @Test
  void complex_class() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class A < B::C\ndef foo()\nend\nend");
    assertTree(tree.identifier()).isIdentifier("A");
    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    assertThat(nativeClassTree).isInstanceOf(NativeTree.class);
    assertThat(nativeClassTree.nativeKind()).isEqualTo(nativeKind("class"));
    assertThat(nativeClassTree.children().get(0)).isEqualTo(tree.identifier());
  }

  @Test
  void namespaced_name() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class A::B\nend");
    assertTree(tree.identifier()).isIdentifier("B");
    Tree nativeClassTree = tree.children().get(0);
    Tree nativeForClassName = nativeClassTree.children().get(0);
    assertThat(nativeForClassName.children().get(1)).isEqualTo(tree.identifier());

    Tree topTreeForScope = nativeForClassName.children().get(0);
    assertTree(topTreeForScope).isIdentifier("A");
  }

}
