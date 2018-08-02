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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RubyNativeKindTest {

  @Test
  public void kinds() {
    String obj = "test";
    RubyNativeKind nativeKind0 = new RubyNativeKind(obj);
    RubyNativeKind nativeKind1 = new RubyNativeKind(String.class);
    RubyNativeKind nativeKind2 = new RubyNativeKind(Object.class);
    RubyNativeKind nativeKind3 = new RubyNativeKind(obj, obj);
    RubyNativeKind nativeKind4 = new RubyNativeKind(String.class, obj);
    RubyNativeKind nativeKind5 = new RubyNativeKind(String.class, "test2");
    RubyNativeKind nativeKind6 = new RubyNativeKind(String.class, "test", "test2");

    assertThat(nativeKind0).isEqualTo(nativeKind0);
    assertThat(nativeKind0).isEqualTo(nativeKind1);
    assertThat(nativeKind0).isNotEqualTo(nativeKind2);
    assertThat(nativeKind0).isNotEqualTo(nativeKind3);
    assertThat(nativeKind0).isNotEqualTo(obj);
    assertThat(nativeKind0).isNotEqualTo(null);
    assertThat(nativeKind3).isEqualTo(nativeKind4);
    assertThat(nativeKind3).isNotEqualTo(nativeKind5);
    assertThat(nativeKind3).isNotEqualTo(nativeKind6);

    assertThat(nativeKind0.hashCode()).isEqualTo(nativeKind1.hashCode());
    assertThat(nativeKind0.hashCode()).isNotEqualTo(nativeKind2.hashCode());
    assertThat(nativeKind0.hashCode()).isNotEqualTo(nativeKind3.hashCode());
    assertThat(nativeKind5.hashCode()).isNotEqualTo(nativeKind6.hashCode());

    assertThat(nativeKind0.toString()).isEqualTo("String");
    assertThat(nativeKind3.toString()).isEqualTo("String[test]");
  }

}