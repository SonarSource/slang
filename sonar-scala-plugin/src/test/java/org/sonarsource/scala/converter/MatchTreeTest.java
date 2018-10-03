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
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.NativeTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class MatchTreeTest extends AbstractScalaConverterTest {

  @Test
  public void simple_match() {
    MatchTree tree = (MatchTree) scalaStatement("x match { case 1=> a case 2=> b }");
    assertTree(tree).isEquivalentTo(slangStatement("match (x) { 1 -> a; 2 -> b; };"));
    assertThat(tree.keyword().text()).isEqualTo("match");
  }

  @Test
  public void default_case() {
    MatchTree tree = (MatchTree) scalaStatement("x match { case 1=> a case _=> b }");
    assertThat(tree.cases().get(1).expression()).isNull();
  }

  @Test
  public void case_with_condition() {
    assertTree(scalaStatement("x match { case 1 if guard => a case 2=> b }")).isInstanceOf(NativeTree.class);
  }
}
