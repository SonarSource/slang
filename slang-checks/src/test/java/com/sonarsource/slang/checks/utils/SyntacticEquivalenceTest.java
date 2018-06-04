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
package com.sonarsource.slang.checks.utils;

import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.parser.SLangConverter;
import java.util.Arrays;
import org.junit.Test;

import static com.sonarsource.slang.checks.utils.SyntacticEquivalence.areEquivalent;
import static com.sonarsource.slang.checks.utils.SyntacticEquivalence.findDuplicatedGroups;
import static org.assertj.core.api.Assertions.assertThat;

public class SyntacticEquivalenceTest {

  private final SLangConverter sLangConverter = new SLangConverter();

  @Test
  public void test_equivalence() {
    assertThat(areEquivalent(parse("1"), parse("1"))).isTrue();
    assertThat(areEquivalent(parse("1"), parse("2"))).isFalse();
    assertThat(areEquivalent(parse("a"), parse("a"))).isTrue();
    assertThat(areEquivalent(parse("a"), parse("b"))).isFalse();
    assertThat(areEquivalent(parse("a"), parse("2"))).isFalse();
    assertThat(areEquivalent(parse("a == 1"), parse("a == 1"))).isTrue();
    assertThat(areEquivalent(parse("a == 1"), parse("a == 2"))).isFalse();
    assertThat(areEquivalent(parse("a == 1"), parse("a >= 1"))).isFalse();
  }

  @Test
  public void duplicateGroups() {
    Tree a1 = parse("a");
    Tree a2 = parse("a");
    Tree b1 = parse("b");
    assertThat(findDuplicatedGroups(Arrays.asList(a1, b1, a2))).containsExactly(Arrays.asList(a1, a2));
    assertThat(findDuplicatedGroups(Arrays.asList(a1, b1))).isEmpty();
  }

  private Tree parse(String code) {
    return sLangConverter.parse(code);
  }

}
