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
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.VariableDeclarationTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class VariableDeclarationTreeTest extends AbstractScalaConverterTest {

  @Test
  public void var_declaration() {
    assertTree(scalaStatement("var x = 42")).isEquivalentTo(slangStatement("var x = 42;"));
  }

  @Test
  public void val_declaration() {
    assertTree(scalaStatement("val x = 42")).isEquivalentTo(slangStatement("val x = 42;"));
  }

  @Test
  public void pattern() {
    assertTree(scalaStatement("val x :: y = my_list")).isInstanceOf(NativeTree.class);
  }

  @Test
  public void modifiers() {
    assertThat(parse("object Obj { final val x = 42 }").descendants().filter(VariableDeclarationTree.class::isInstance)).isEmpty();
  }

  @Test
  public void fields() {
    assertThat(scalaStatement("new { val x = 42 }").descendants().filter(VariableDeclarationTree.class::isInstance)).isEmpty();
    assertThat(scalaStatement("new { var x = 42 }").descendants().filter(VariableDeclarationTree.class::isInstance)).isEmpty();
  }
}
