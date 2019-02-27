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
package org.sonarsource.scala.converter;

import org.junit.Test;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class ClassDeclarationTreeTest extends AbstractScalaConverterTest {
  @Test
  public void empty_class() {
    Tree tree = scalaStatement("class Foo {}");
    assertTree(tree).isEquivalentTo(scalaStatement("class Foo {}"));
    assertTree(tree).isNotEquivalentTo(scalaStatement("class FooBar {}"));
    assertTree(tree).isNotEquivalentTo(scalaStatement("class Foo { def bar() {}}"));
    ClassDeclarationTree classTree = (ClassDeclarationTree) tree;
    assertThat(classTree.identifier().name()).isEqualTo("Foo");
  }

  @Test
  public void class_with_method() {
    Tree tree = scalaStatement("class Foo { def bar() {} }");
    assertTree(tree).isEquivalentTo(scalaStatement("class Foo { def bar() {} }"));
    assertTree(tree).isNotEquivalentTo(scalaStatement("class Foo { def foo() {} }"));
    ClassDeclarationTree classTree = (ClassDeclarationTree) tree;
    assertThat(classTree.identifier().name()).isEqualTo("Foo");
  }

}
