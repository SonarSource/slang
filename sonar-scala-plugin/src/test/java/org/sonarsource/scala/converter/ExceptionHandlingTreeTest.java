/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.ExceptionHandlingTree;
import org.sonarsource.slang.api.IdentifierTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class ExceptionHandlingTreeTest extends AbstractScalaConverterTest {

  @Test
  void trivial_try() {
    ExceptionHandlingTree tryCatch = (ExceptionHandlingTree) scalaStatement("try { a }");
    assertThat(tryCatch.tryKeyword().text()).isEqualTo("try");
    assertTree(tryCatch.tryBlock()).isBlock(IdentifierTree.class);
    assertThat(tryCatch.catchBlocks()).isEmpty();
    assertThat(tryCatch.finallyBlock()).isNull();
  }

  @Test
  void try_catch() {
    ExceptionHandlingTree tryCatch = (ExceptionHandlingTree) scalaStatement("try { a } catch { b }");
    assertThat(tryCatch.tryKeyword().text()).isEqualTo("try");
    assertTree(tryCatch.tryBlock()).isBlock(IdentifierTree.class);
    assertThat(tryCatch.catchBlocks()).hasSize(1);
    assertThat(tryCatch.finallyBlock()).isNull();
    assertThat(identifierDescendants(tryCatch.catchBlocks().get(0))).containsExactly("b");
  }

  @Test
  void try_catch_case() {
    ExceptionHandlingTree tryCatch = (ExceptionHandlingTree) scalaStatement("try { a } catch { case e => b }");
    assertTree(tryCatch).isEquivalentTo(scalaStatement("try { a } catch { case e => b }"));
    assertThat(tryCatch.catchBlocks()).hasSize(1);
    assertThat(tryCatch.finallyBlock()).isNull();
    assertThat(identifierDescendants(tryCatch.catchBlocks().get(0))).containsExactly("e", "b");
  }

  @Test
  void try_finally() {
    ExceptionHandlingTree tryCatch = (ExceptionHandlingTree) scalaStatement("try { a } finally { b }");
    assertThat(tryCatch.tryKeyword().text()).isEqualTo("try");
    assertTree(tryCatch.tryBlock()).isBlock(IdentifierTree.class);
    assertThat(tryCatch.catchBlocks()).isEmpty();
    assertTree(tryCatch.finallyBlock()).isBlock(IdentifierTree.class);
  }
}
