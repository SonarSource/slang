/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
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
package org.sonarsource.slang.impl;

import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.ImportDeclarationTree;
import org.sonarsource.slang.api.Tree;

import static org.sonarsource.slang.utils.TreeCreationUtils.identifier;

class ImportDeclarationTreeImplTest {

  @Test
  void test() {
    Tree identifier = identifier("x");
    ImportDeclarationTree tree = new ImportDeclarationTreeImpl(null, Collections.singletonList(identifier));
    Assertions.assertThat(tree.children()).containsExactly(identifier);
  }
}
