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
package org.sonarsource.ruby.converter.visitor;


import org.junit.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.VariableDeclarationTree;

import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class VariableDeclarationVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void declaration() {
    Tree tree = rubyStatement("def foo; a = 1; end; def self.bar; a = 1; end");
    assertTree(tree).isInstanceOf(BlockTree.class);
    FunctionDeclarationTree fooFunction = ((FunctionDeclarationTree) tree.children().get(0));
    assertTree(fooFunction.body().children().get(0)).isEquivalentTo(slangStatement("var a = 1;"));
    FunctionDeclarationTree barFunction = ((FunctionDeclarationTree) tree.children().get(1));
    assertTree(barFunction.body().children().get(0)).isEquivalentTo(slangStatement("var a = 1;"));
  }

  @Test
  public void class_scope() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class Test\n" +
      "    fooVar = 10\n" +  // variable declaration
      "    barVar = 10\n" +  // variable declaration
      "\n" +
      "    define_method :foo do \n" +
      "      fooVar = 2\n" +  // assignment
      "      puts fooVar\n" +
      "    end\n" +
      "\n" +
      "def bar; fooVar = 1; end\n" + // variable declaration shadowing one from class
      "barVar = 10\n" +  // re-assignment
      "end");
    Tree body = tree.classTree().children().get(1);
    assertTree(body.children().get(0)).isInstanceOf(VariableDeclarationTree.class);  // fooVar
    assertTree(body.children().get(1)).isInstanceOf(VariableDeclarationTree.class);  // barVar

    BlockTree block = (BlockTree) body.children().get(2).children().get(1);
    assertTree(block.statementOrExpressions().get(0)).isInstanceOf(AssignmentExpressionTree.class); // fooVar

    FunctionDeclarationTree functionDeclarationTree = (FunctionDeclarationTree) body.children().get(3);
    assertTree(functionDeclarationTree.body().statementOrExpressions().get(0)).isInstanceOf(VariableDeclarationTree.class); // fooVar

    assertTree(body.children().get(4)).isInstanceOf(AssignmentExpressionTree.class); // barVar
  }

}
