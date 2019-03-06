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

import java.util.List;
import org.junit.Test;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.Tree;

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
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement("def foo(p1: String)(p2: Int) = { println(param) }");
    assertThat(func.formalParameters()).hasSize(2);
    assertTree(func.formalParameters().get(0)).hasTokens("p1", ":", "String");
    assertTree(func.formalParameters().get(1)).hasTokens("p2", ":", "Int");
  }

  @Test
  public void function_parameters_test() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(p1: String) = param");
    assertTree(func).hasParameterNames("p1");
    assertTree(func.formalParameters().get(0)).isInstanceOf(ParameterTree.class);
  }

  @Test
  public void function_multiples_parameters_test() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(p1: String, p2: String, p3: String) = {p1}");
    assertTree(func).hasParameterNames("p1", "p2", "p3");
  }

  @Test
  public void function_default_parameter() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(p1: String = \"def\") = {p1}");
    assertThat(func.formalParameters()).hasSize(1);
    assertTree(func).hasParameterNames("p1");
    assertTree(func.formalParameters().get(0)).isInstanceOf(ParameterTree.class);
    ParameterTree parameter1 = (ParameterTree) func.formalParameters().get(0);
    assertTree(parameter1.defaultValue()).isLiteral("\"def\"");
  }

  @Test
  public void function_multiple_parameters_with_default() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(p1: String = \"def\", p2: String, p3: String = \"def2\") = {p1}");
    assertThat(func.formalParameters()).hasSize(3);
    assertTree(func).hasParameterNames("p1", "p2", "p3");
    ParameterTree p1 = (ParameterTree) func.formalParameters().get(0);
    ParameterTree p2 = (ParameterTree) func.formalParameters().get(1);
    ParameterTree p3 = (ParameterTree) func.formalParameters().get(2);
    assertTree(p1.defaultValue()).isLiteral("\"def\"");
    assertTree(p2.defaultValue()).isNull();
    assertTree(p3.defaultValue()).isLiteral("\"def2\"");
  }

  @Test
  public void function_implicit_parameter() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(implicit p1: String) = {p1}");
    assertThat(func.formalParameters()).hasSize(1);
    assertTree(func.formalParameters().get(0)).isInstanceOf(ParameterTree.class);
    assertThat(((ParameterTree)func.formalParameters().get(0)).modifiers()).hasSize(1);
    assertThat(((ParameterTree)func.formalParameters().get(0)).modifiers().get(0)).isInstanceOf(NativeTree.class);
  }

  @Test
  public void function_annotated_parameter() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
      "def foo(@transient p1: String) = {p1}");
    assertThat(func.formalParameters()).hasSize(1);
    assertTree(func.formalParameters().get(0)).isInstanceOf(ParameterTree.class);
    assertThat(((ParameterTree)func.formalParameters().get(0)).modifiers()).hasSize(1);
    assertThat(((ParameterTree)func.formalParameters().get(0)).modifiers().get(0)).isInstanceOf(NativeTree.class);
  }

  @Test
  public void function_with_annotated_and_implicit_parameter() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(implicit @transient p1: String) = {p1}");
    assertThat(func.formalParameters()).hasSize(1);
    Tree parameterTree = func.formalParameters().get(0);
    assertTree(parameterTree).isInstanceOf(ParameterTree.class);
    List<Tree> modifiers = ((ParameterTree) parameterTree).modifiers();
    assertThat(modifiers).hasSize(2);
    assertTree(modifiers.get(0)).hasTokens("@", "transient");
    assertTree(modifiers.get(1)).hasTokens("implicit");
  }

  @Test
  public void function_with_annotated_and_implicit_parameters() {
    FunctionDeclarationTree func = (FunctionDeclarationTree) scalaStatement(
        "def foo(p1 : Int)(implicit @transient p2 : Char, @transient p3: String) = {p1}");
    assertThat(func.formalParameters()).hasSize(3);
    ParameterTree p1 = (ParameterTree)func.formalParameters().get(0);
    ParameterTree p2 = (ParameterTree)func.formalParameters().get(1);
    ParameterTree p3 = (ParameterTree)func.formalParameters().get(2);
    assertTree(p1.identifier()).isIdentifier("p1");
    assertThat(p1.modifiers()).isEmpty();
    assertTree(p2.identifier()).isIdentifier("p2");
    assertThat(p2.modifiers()).hasSize(2);
    // Note: Scalameta add "implicit" modifier after annotations and not before, but currently it does not worth to be fixed
    assertTree(p2.modifiers().get(0)).hasTokens("@", "transient");
    assertTree(p2.modifiers().get(1)).hasTokens("implicit");
    assertTree(p3.identifier()).isIdentifier("p3");
    assertThat(p3.modifiers()).hasSize(2);
    assertTree(p3.modifiers().get(0)).hasTokens("@", "transient");
    assertTree(p3.modifiers().get(1)).hasTokens("implicit");
  }

  @Test
  public void modifiers() {
    FunctionDeclarationTree privateFunc = scalaMethod(
      "private def foo(p1: String) = {p1}");
    assertThat(privateFunc.modifiers()).hasSize(1);
    ModifierTree modifier = (ModifierTree) privateFunc.modifiers().get(0);
    assertThat(modifier.kind()).isEqualTo(ModifierTree.Kind.PRIVATE);

    FunctionDeclarationTree overriddenFunc = scalaMethod(
      "override def foo(p1: String) = {p1}");
    assertThat(overriddenFunc.modifiers()).hasSize(1);
    modifier = (ModifierTree) overriddenFunc.modifiers().get(0);
    assertThat(modifier.kind()).isEqualTo(ModifierTree.Kind.OVERRIDE);

    FunctionDeclarationTree publicFunc = scalaMethod(
      "def foo(p1: String) = {p1}");
    assertThat(publicFunc.modifiers()).hasSize(0);
  }

}
