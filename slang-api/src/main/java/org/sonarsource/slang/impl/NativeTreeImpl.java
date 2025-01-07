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

import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.List;

public class NativeTreeImpl extends BaseTreeImpl implements NativeTree {

  private final NativeKind nativeKind;
  private final List<Tree> children;

  public NativeTreeImpl(TreeMetaData metaData, NativeKind nativeKind, List<Tree> children) {
    super(metaData);
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
