/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.utils.SyntacticEquivalence;
import java.util.List;
import org.assertj.core.api.AbstractAssert;

import static org.sonarsource.slang.visitors.TreePrinter.tree2string;
import static org.assertj.core.api.Assertions.assertThat;

public class TreesAssert extends AbstractAssert<TreesAssert, List<Tree>> {

  public TreesAssert(List<Tree> actual) {
    super(actual, TreesAssert.class);
  }

  public static TreesAssert assertTrees(List<Tree> actual) {
    return new TreesAssert(actual);
  }

  public TreesAssert isEquivalentTo(List<Tree> expected) {
    isNotNull();
    boolean equivalent = SyntacticEquivalence.areEquivalent(actual, expected);
    if (!equivalent) {
      assertThat(tree2string(actual)).isEqualTo(tree2string(expected));
      failWithMessage("Expected tree: <%s>\nbut was: <%s>", tree2string(expected), tree2string(actual));
    }
    return this;
  }

  public TreesAssert isNotEquivalentTo(List<Tree> expected) {
    isNotNull();
    boolean equivalent = SyntacticEquivalence.areEquivalent(actual, expected);
    if (equivalent) {
      assertThat(tree2string(actual)).isNotEqualTo(tree2string(expected));
      failWithMessage("Expected <%s> to not be equivalent to <%s>", actual, expected);
    }
    return this;
  }

}
