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
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.VariableDeclarationTree;

import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class IdentifierVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void test() throws Exception {
    assertTree(((AssignmentExpressionTree) rubyStatement("$a = 1")).leftHandSide()).isIdentifier("$a");
    assertTree(((AssignmentExpressionTree) rubyStatement("@a = 1")).leftHandSide()).isIdentifier("@a");
    assertTree(((AssignmentExpressionTree) rubyStatement("@@a = 1")).leftHandSide()).isIdentifier("@@a");
    assertTree(((VariableDeclarationTree) rubyStatement("A = 1")).identifier()).isIdentifier("A");

    assertTree(((VariableDeclarationTree) rubyStatement("a = a")).initializer()).isIdentifier("a");
    assertTree(((VariableDeclarationTree) rubyStatement("a = b")).initializer()).isInstanceOf(NativeTree.class);

    assertTree(((VariableDeclarationTree) rubyStatement("a = @b")).initializer()).isIdentifier("@b");
    assertTree(((VariableDeclarationTree) rubyStatement("a = @@b")).initializer()).isIdentifier("@@b");
    assertTree(((VariableDeclarationTree) rubyStatement("a = B")).initializer()).isIdentifier("B");

  }
}
