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
package org.sonarsource.scala.converter;

import org.junit.Test;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class IfTreeTest extends AbstractScalaConverterTest {

  @Test
  public void if_without_else() {
    Tree tree = scalaStatement("if (x) { 42 }");
    assertTree(tree).isEquivalentTo(slangStatement("if (x) { 42; };"));
    IfTree ifTree = (IfTree) tree;
    assertThat(ifTree.ifKeyword().text()).isEqualTo("if");
    assertThat(ifTree.elseBranch()).isNull();
    assertThat(ifTree.elseKeyword()).isNull();
  }

  @Test
  public void if_with_else() {
    Tree tree = scalaStatement("if (x) { 42 } else { 43 }");
    assertTree(tree).isEquivalentTo(slangStatement("if (x) { 42; } else { 43 };"));
    IfTree ifTree = (IfTree) tree;
    assertThat(ifTree.ifKeyword().text()).isEqualTo("if");
    assertThat(ifTree.elseBranch()).isNotNull();
    assertThat(ifTree.elseKeyword().text()).isEqualTo("else");
  }
}
