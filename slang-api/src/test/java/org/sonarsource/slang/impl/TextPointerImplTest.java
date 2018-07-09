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
package org.sonarsource.slang.impl;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TextPointerImplTest {

  @Test
  public void test_equals() {
    TextPointerImpl p1 = new TextPointerImpl(1, 2);
    assertThat(p1).isEqualTo(p1);
    assertThat(p1).isEqualTo(new TextPointerImpl(1, 2));
    assertThat(p1).isNotEqualTo(new TextPointerImpl(1, 3));
    assertThat(p1).isNotEqualTo(new TextPointerImpl(3, 2));
    assertThat(p1).isNotEqualTo(null);
    assertThat(p1).isNotEqualTo("");
  }

  @Test
  public void test_hashCode() {
    assertThat(new TextPointerImpl(1, 2).hashCode()).isEqualTo(new TextPointerImpl(1, 2).hashCode());
    assertThat(new TextPointerImpl(1, 2).hashCode()).isNotEqualTo(new TextPointerImpl(1, 3).hashCode());
  }

  @Test
  public void test_compareTo() {
    assertThat(new TextPointerImpl(1, 2).compareTo(new TextPointerImpl(1, 2))).isEqualTo(0);
    assertThat(new TextPointerImpl(1, 2).compareTo(new TextPointerImpl(1, 4))).isEqualTo(-1);
    assertThat(new TextPointerImpl(1, 2).compareTo(new TextPointerImpl(2, 1))).isEqualTo(-1);
    assertThat(new TextPointerImpl(1, 2).compareTo(new TextPointerImpl(1, 1))).isEqualTo(1);
  }
}
