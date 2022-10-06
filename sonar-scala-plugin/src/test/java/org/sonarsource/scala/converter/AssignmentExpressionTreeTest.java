/*
 * SonarSource SLang
 * Copyright (C) 2018-2022 SonarSource SA
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

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class AssignmentExpressionTreeTest extends AbstractScalaConverterTest {

  @Test
  void assignment() {
    assertTree(scalaStatement("x = 42")).isEquivalentTo(slangStatement("x = 42;"));
    assertTree(scalaStatement("x += 42")).isNotEquivalentTo(slangStatement("x = 42;"));
  }

  @Test
  void named_argument() {
    assertThat(assignmentDescendants(scalaStatement("foo(param = 42)"))).isEmpty();
    assertThat(assignmentDescendants(scalaStatement("new MyClass(param = 42)"))).isEmpty();
    assertThat(assignmentDescendants(parse("class A { def this() = this(param = 42) }"))).isEmpty();
  }

  private Stream<Tree> assignmentDescendants(Tree tree) {
    return tree.descendants().filter(AssignmentExpressionTree.class::isInstance);
  }

}
