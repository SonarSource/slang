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

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class Scala3TreeTest extends AbstractScalaConverterTest {

  @Test
  void opaque_types() {
    Tree tree = parse(
      "package object Opaquetypes {\n" +
      "  opaque type Logarithm = Double\n" +
      "}");
    Tree packageObject = tree.children().get(0);
    assertThat(packageObject.children().get(0))
      .isInstanceOf(IdentifierTree.class);

    Tree opaquePart = packageObject.children().get(1).children().get(0).children().get(0);
    assertThat(opaquePart).isInstanceOf(NativeTree.class);
    NativeTree opaque = (NativeTree) opaquePart;
    assertThat(opaque.nativeKind()).hasToString("ScalaNativeKind(class scala.meta.Mod$Opaque$ModOpaqueImpl)");
  }

}
