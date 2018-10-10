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
import org.sonarsource.slang.api.CodeVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class ScalaCodeVerifierTest {

  private final CodeVerifier verifier = new ScalaCodeVerifier();

  @Test
  public void testContainsCode() {
    assertThat(verifier.containsCode("val hello = 1")).isTrue();
    assertThat(verifier.containsCode("if (cond) true else false")).isTrue();

    assertThat(verifier.containsCode("")).isFalse();
    assertThat(verifier.containsCode("  ")).isFalse();

    assertThat(verifier.containsCode("1.0")).isFalse();

    assertThat(verifier.containsCode("hello")).isFalse();
    assertThat(verifier.containsCode("hello world")).isFalse();
    assertThat(verifier.containsCode("hello world and")).isFalse();
    assertThat(verifier.containsCode("hello world and John")).isFalse();
    assertThat(verifier.containsCode("TODO: remove when cats.Foldable support export-hook")).isFalse();
    assertThat(verifier.containsCode("abc:off")).isFalse();
    assertThat(verifier.containsCode("TODO: put something meaningful here?")).isFalse();
    assertThat(verifier.containsCode("1: The previous character was CR")).isFalse();
    assertThat(verifier.containsCode("Replace charset= parameter(s)")).isFalse();
    assertThat(verifier.containsCode("* Set Content-Type header to application/json;charset=utf-8")).isFalse();
    assertThat(verifier.containsCode("RSPEC-325")).isFalse();

    assertThat(verifier.containsCode("return something very useful")).isFalse();
    assertThat(verifier.containsCode("return foo(bar, baz)")).isTrue();

    assertThat(verifier.containsCode("new line")).isFalse();
    assertThat(verifier.containsCode("new line(x, y)")).isTrue();

    assertThat(verifier.containsCode("try something different")).isFalse();
    assertThat(verifier.containsCode("try x catch { y }")).isTrue();

    assertThat(verifier.containsCode("exec(param1, param2)")).isTrue();
    assertThat(verifier.containsCode("exec(\n param1, param2)")).isTrue();
    assertThat(verifier.containsCode("exec(x.y(z))")).isTrue();
    assertThat(verifier.containsCode("done (almost)")).isFalse();
    assertThat(verifier.containsCode("done (almost done)")).isFalse();

    assertThat(verifier.containsCode("array[int]")).isFalse();
    assertThat(verifier.containsCode("(x, y) => z")).isFalse();
    assertThat(verifier.containsCode("* (42)")).isFalse();
    assertThat(verifier.containsCode("(name, persons)")).isFalse();

    assertThat(verifier.containsCode("case None => true")).isTrue();
  }
}
