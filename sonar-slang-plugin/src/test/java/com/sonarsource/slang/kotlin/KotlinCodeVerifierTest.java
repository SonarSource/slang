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
package com.sonarsource.slang.kotlin;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KotlinCodeVerifierTest {
  private KotlinCodeVerifier kotlinCodeVerifier = new KotlinCodeVerifier();
  @Test
  public void testContainsCode() {
    String comment = "This is a normal sentence: definitely not code";
    assertThat(kotlinCodeVerifier.containsCode(comment)).isFalse();
    String commentWithoutPunctuation = "this is a normal comment";
    assertThat(kotlinCodeVerifier.containsCode(commentWithoutPunctuation)).isFalse();
    String commentInfix = "just three words";
    assertThat(kotlinCodeVerifier.containsCode(commentInfix)).isFalse();
    String commentWithCodeKeywords = "description for foo(\"hello world\")";
    assertThat(kotlinCodeVerifier.containsCode(commentWithCodeKeywords)).isFalse();
    String commentWithCode = "foo(\"hello world\")";
    assertThat(kotlinCodeVerifier.containsCode(commentWithCode)).isTrue();
    String commentWithCode2 = "this is a + b";
    assertThat(kotlinCodeVerifier.containsCode(commentWithCode2)).isFalse();
    String commentWithCode3 = "The user name is empty";
    assertThat(kotlinCodeVerifier.containsCode(commentWithCode3)).isFalse();
    String literal = "SNAPSHOT";
    assertThat(kotlinCodeVerifier.containsCode(literal)).isFalse();
    String infix = "1 shl 2";
    assertThat(kotlinCodeVerifier.containsCode(infix)).isFalse();
    String infix2 = "1 shl foo";
    assertThat(kotlinCodeVerifier.containsCode(infix2)).isFalse();
    String kdocComment = "* @return foo(bar)";
    assertThat(kotlinCodeVerifier.containsCode(kdocComment)).isFalse();

    String text = "only unlocked states";
    assertThat(kotlinCodeVerifier.containsCode(text)).isFalse();

    String text2 = "The identity represented by this set of mock services. Defaults to a test identity.\n" +
      " You can also use the alternative parameter initialIdentityName which accepts a\n" +
      " [CordaX500Name]";
    assertThat(kotlinCodeVerifier.containsCode(text2)).isFalse();
    String text3 = " 0\n 1";
    assertThat(kotlinCodeVerifier.containsCode(text3)).isFalse();
    String text4 = " this";
    assertThat(kotlinCodeVerifier.containsCode(text4)).isFalse();

    String text5 = " --- check";
    assertThat(kotlinCodeVerifier.containsCode(text5)).isFalse();
    String text6 = "exposed as public";
    assertThat(kotlinCodeVerifier.containsCode(text6)).isFalse();

    String text7 = "foo.rs";
    assertThat(kotlinCodeVerifier.containsCode(text7)).isFalse();



  }
}