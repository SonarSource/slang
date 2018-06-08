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

import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class MatchCaseTreeImpl extends BaseTreeImpl implements MatchCaseTree {

  private final Tree expression;
  private final Tree body;

  public MatchCaseTreeImpl(TreeMetaData metaData, @Nullable Tree expression, Tree body) {
    super(metaData);
    this.expression = expression;
    this.body = body;
  }

  @CheckForNull
  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public Tree body() {
    return body;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    if (expression != null) {
      children.add(expression);
    }
    children.add(body);
    return children;
  }
}
