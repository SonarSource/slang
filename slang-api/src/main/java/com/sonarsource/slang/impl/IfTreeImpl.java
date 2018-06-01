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
package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;

public class IfTreeImpl extends BaseTreeImpl implements IfTree {

  private final Tree condition;
  private final Tree thenBranch;
  private final Tree elseBranch;

  public IfTreeImpl(TextRange textRange, Tree condition, Tree thenBranch, Tree elseBranch) {
    super(textRange);
    this.condition = condition;
    this.thenBranch = thenBranch;
    this.elseBranch = elseBranch;
  }

  @Override
  public Tree condition() {
    return condition;
  }

  @Override
  public Tree thenBranch() {
    return thenBranch;
  }

  @CheckForNull
  @Override
  public Tree elseBranch() {
    return elseBranch;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(condition);
    children.add(thenBranch);
    if (elseBranch != null) {
      children.add(elseBranch);
    }
    return children;
  }
}
