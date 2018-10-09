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
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.ParameterTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class FunctionDeclarationTreeTest extends AbstractScalaConverterTest {

  @Test
  public void function() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
      "def foo(p1: String, p2: Int): Unit = { println(param) }");
    assertTree(func.name()).isIdentifier("foo");
    assertThat(func.formalParameters()).hasSize(2);
    assertThat(func.returnType()).isNotNull();
    assertThat(func.body().statementOrExpressions()).hasSize(1);
  }

  @Test
  public void function_without_return_type() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
      "def foo(p1: String, p2: Int) = { println(param) }");
    assertThat(func.returnType()).isNull();
  }

  @Test
  public void function_with_simple_body() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
      "def foo(p1: String, p2: Int) = param");
    assertThat(func.body().statementOrExpressions()).hasSize(1);
    assertTree(func.body().statementOrExpressions().get(0)).isIdentifier("param");
  }

  @Test
  public void function_without_parameter_list() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
      "def foo = { println(param) }");
    assertThat(func.formalParameters()).isEmpty();
  }

  @Test
  public void function_with_multiple_parameter_lists() {
    assertThat(scalaStatement("def foo(p1: String)(p2: Int) = { println(param) }")).isInstanceOf(NativeTree.class);
  }

  @Test
  public void function_arguments_test() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(p1: String) = param");
    assertTree(func).hasParameterNames("p1");
    assertTree(func.formalParameters().get(0)).isInstanceOf(ParameterTree.class);
  }

  @Test
  public void function_multiples_arguments_test() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(p1: String, p2: String, p3: String) = {p1}");
    assertTree(func).hasParameterNames("p1", "p2", "p3");
  }

  @Test
  public void function_default_argument() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(p1: String = \"def\") = {p1}");
    assertThat(func.formalParameters()).hasSize(1);
    assertTree(func.formalParameters().get(0)).isInstanceOf(NativeTree.class);
  }

  @Test
  public void function_multiple_argument_with_default() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(p1: String = \"def\", p2: String, p3: String = \"def\") = {p1}");
    assertThat(func.formalParameters()).hasSize(3);
    assertTree(func).hasParameterNames("p2");
  }

  @Test
  public void function_implicit_argument() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(implicit p1: String) = {p1}");
    assertThat(func.formalParameters()).hasSize(1);
    assertTree(func.formalParameters().get(0)).isInstanceOf(NativeTree.class);
  }
}
