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


import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.log.LogTester;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class RubyProcessorTest {

  private static RubyConverter converter;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

  @BeforeClass
  public static void setUp() {
    converter = new RubyConverter();
  }

  @BeforeClass
  public static void tearDown() {
    converter.terminate();
  }

  @Test
  public void class_tree() {
    Tree topLevel = converter.parse("class A < B; def foo() end; end");
    assertTree(topLevel).isInstanceOf(TopLevelTree.class);

    assertThat(topLevel.children()).hasSize(1);
    Tree classTree = topLevel.children().get(0);
    assertTree(classTree).isInstanceOf(ClassDeclarationTree.class);
    assertThat(((ClassDeclarationTree) classTree).identifier().name()).isEqualTo("A");
  }

  @Test
  public void singleton_class() {
    Tree topLevel = converter.parse("class << a; end");
    assertTree(topLevel).isInstanceOf(TopLevelTree.class);

    assertThat(topLevel.children()).hasSize(1);
    Tree classTree = topLevel.children().get(0);
    assertTree(classTree).isInstanceOf(NativeTree.class);
  }
}
