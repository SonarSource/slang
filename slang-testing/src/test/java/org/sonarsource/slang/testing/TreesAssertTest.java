/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.testing;

import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonarsource.slang.testing.TreesAssert.assertTrees;
import static java.util.Collections.singletonList;

class TreesAssertTest {

  private static final LiteralTreeImpl LITERAL_42 = new LiteralTreeImpl(null, "42");

  @Test
  void equivalent_ok() {
    assertTrees(singletonList(LITERAL_42)).isEquivalentTo(singletonList(new LiteralTreeImpl(null, "42")));
  }

  @Test
  void equivalent_failure() {
    assertThrows(AssertionError.class,
      () -> assertTrees(singletonList(LITERAL_42)).isEquivalentTo(singletonList(new LiteralTreeImpl(null, "43"))));
  }

  @Test
  void notequivalent_ok() {
    assertTrees(singletonList(LITERAL_42)).isNotEquivalentTo(singletonList(new LiteralTreeImpl(null, "43")));
  }

  @Test
  void notequivalent_failure() {
    assertThrows(AssertionError.class,
      () -> assertTrees(singletonList(LITERAL_42)).isNotEquivalentTo(singletonList(new LiteralTreeImpl(null, "42"))));
  }

}
