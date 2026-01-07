/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
package org.sonarsource.slang.visitors;

import org.sonarsource.slang.api.Tree;
import java.util.ArrayDeque;
import java.util.Deque;

public class TreeContext {

  private final Deque<Tree> ancestors;
  private Tree current;

  public TreeContext() {
    ancestors = new ArrayDeque<>();
  }

  public Deque<Tree> ancestors() {
    return ancestors;
  }

  protected void before(Tree root) {
    ancestors.clear();
  }

  public void enter(Tree node) {
    if (current != null) {
      ancestors.push(current);
    }
    current = node;
  }

  public void leave(Tree node) {
    if (!ancestors.isEmpty()) {
      current = ancestors.pop();
    }
  }

}
