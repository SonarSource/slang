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

import com.sonarsource.slang.api.NativeKind;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.Tree;
import java.util.List;

public class NativeTreeImpl implements NativeTree {

  private final NativeKind nativeKind;
  private final List<Tree> children;

  public NativeTreeImpl(NativeKind nativeKind, List<Tree> children) {
    this.nativeKind = nativeKind;
    this.children = children;
  }

  @Override
  public NativeKind nativeKind() {
    return nativeKind;
  }

  @Override
  public List<Tree> children() {
    return children;
  }
}
