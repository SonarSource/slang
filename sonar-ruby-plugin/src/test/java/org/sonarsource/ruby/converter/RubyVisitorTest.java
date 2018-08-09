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
package org.sonarsource.ruby.converter;


import org.junit.Test;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.sonarsource.slang.testing.TreesAssert.assertTrees;

public class RubyVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void simple_class() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class A\ndef foo()\nend\nend");
    assertTree(tree.identifier()).isIdentifier("A");
    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    assertThat(nativeClassTree).isInstanceOf(NativeTree.class);
    assertThat(nativeClassTree.nativeKind()).isEqualTo(nativeKind("class"));
    assertThat(nativeClassTree.children().get(0)).isEqualTo(tree.identifier());
  }

  @Test
  public void complex_class() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class A < B::C\ndef foo()\nend\nend");
    assertTree(tree.identifier()).isIdentifier("A");
    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    assertThat(nativeClassTree).isInstanceOf(NativeTree.class);
    assertThat(nativeClassTree.nativeKind()).isEqualTo(nativeKind("class"));
    assertThat(nativeClassTree.children().get(0)).isEqualTo(tree.identifier());
  }

  @Test
  public void simple_function() {
    FunctionDeclarationTree tree = (FunctionDeclarationTree) rubyStatement("def foo(p)\n puts 'hello'\nend");
    assertTree(tree.name()).isIdentifier("foo");
    assertThat(tree.modifiers()).isEmpty();
    assertThat(tree.returnType()).isNull();
    assertThat(tree.formalParameters()).hasSize(1);
    assertTree(tree.formalParameters().get(0)).isEquivalentTo(nativeTree(nativeKind("arg"), asList(nativeTree(nativeKind("p")))));
    assertThat(tree.body().statementOrExpressions()).hasSize(1);
    assertThat(((NativeTree) tree.body().statementOrExpressions().get(0)).nativeKind()).isEqualTo(nativeKind("send"));
    assertThat(tree.nativeChildren()).isEmpty();
  }

  @Test
  public void function_without_arguments() {
    FunctionDeclarationTree tree = (FunctionDeclarationTree) rubyStatement("def foo; end");
    assertTree(tree.name()).isIdentifier("foo");
    assertThat(tree.modifiers()).isEmpty();
    assertThat(tree.returnType()).isNull();
    assertThat(tree.formalParameters()).isEmpty();
    assertThat(tree.body()).isNull();
    assertThat(tree.nativeChildren()).isEmpty();
  }

  @Test
  public void singleton_method() {
    FunctionDeclarationTree tree = (FunctionDeclarationTree) rubyStatement("def a.foo\n puts 'hello'\n puts 'hello'\nend");
    assertTree(tree.name()).isIdentifier("foo");
    assertThat(tree.modifiers()).isEmpty();
    assertThat(tree.returnType()).isNull();
    assertThat(tree.formalParameters()).isEmpty();
    assertThat(tree.body().statementOrExpressions()).hasSize(1);
    assertThat(((NativeTree) tree.body().statementOrExpressions().get(0)).nativeKind()).isEqualTo(nativeKind("begin"));
    assertThat(tree.nativeChildren()).hasSize(1);
    assertTree(tree.nativeChildren().get(0)).isEquivalentTo(nativeTree(nativeKind("send"), asList(nativeTree(nativeKind("a")))));
  }

  @Test
  public void top_level_tree() {
    assertTree(converter.parse(("true\nfalse"))).isInstanceOf(TopLevelTree.class);
    assertTree(converter.parse(("true\r\nfalse"))).isInstanceOf(TopLevelTree.class);
  }

  @Test
  public void parse_with_missing_node() {
    Tree tree = converter.parse("def is_root?\nend"); // method has null argument list
    assertThat(tree).isNotNull();
  }

  @Test
  public void singletons() {
    assertTree(rubyStatement("true")).isEquivalentTo(nativeTree(nativeKind("true"), emptyList()));
    assertTree(rubyStatement("false")).isEquivalentTo(nativeTree(nativeKind("false"), emptyList()));
    assertTree(rubyStatement("nil")).isEquivalentTo(nativeTree(nativeKind("nil"), emptyList()));
  }

  @Test
  public void int_literals() {
    assertTrees(rubyStatements("2; 512; 4\n2431323"))
      .isEquivalentTo(slangStatements("2; 512; 4; 2431323;"));
    assertTree(rubyStatement("2")).isLiteral("2");

    // literal bigger than Long.MAX_VALUE are returned as BigInteger by JRuby
    assertTree(rubyStatement("10000000000000000000")).isLiteral("10000000000000000000");
  }

}
