/*
 * SonarSource SLang
 * Copyright (C) 2018-2019 SonarSource SA
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
package org.sonarsource.scala.checks;

import org.junit.Test;

public class UnusedPrivateMethodScalaCheckTest {

  @Test
  public void test_with_companion_object() {
    ScalaVerifier.verify("UnusedPrivateMethodScala.scala", new UnusedPrivateMethodScalaCheck());
  }

  @Test
  public void test() {
    ScalaVerifier.verify("UnusedPrivateMethodScala.NoCompanion.scala", new UnusedPrivateMethodScalaCheck());
  }

  @Test
  public void test_inner_class() {
    ScalaVerifier.verify("UnusedPrivateMethodScala.InnerClass.scala", new UnusedPrivateMethodScalaCheck());
  }

  @Test
  public void test_class_in_package() {
    ScalaVerifier.verify("UnusedPrivateMethodScala.Package.scala", new UnusedPrivateMethodScalaCheck());
  }
}
