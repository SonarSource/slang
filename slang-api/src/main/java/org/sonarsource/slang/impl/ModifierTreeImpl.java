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

import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Collections;
import java.util.List;

public class ModifierTreeImpl extends BaseTreeImpl implements ModifierTree {

  private final Kind kind;

  public ModifierTreeImpl(TreeMetaData metaData, Kind kind) {
    super(metaData);
    this.kind = kind;
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  public List<Tree> children() {
    return Collections.emptyList();
  }

}
