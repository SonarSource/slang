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

import com.sonarsource.slang.api.CatchTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CatchTreeImpl extends BaseTreeImpl implements CatchTree {

  private final Tree catchParameter;
  private final Tree catchBlock;

  public CatchTreeImpl(TreeMetaData metaData, @Nullable Tree catchParameter, Tree catchBlock) {
    super(metaData);
    this.catchParameter = catchParameter;
    this.catchBlock = catchBlock;
  }

  @CheckForNull
  @Override
  public Tree catchParameter() {
    return catchParameter;
  }

  @Override
  public Tree catchBlock() {
    return catchBlock;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();

    if (catchParameter != null) {
      children.add(catchParameter);
    }
    children.add(catchBlock);

    return children;
  }
}
