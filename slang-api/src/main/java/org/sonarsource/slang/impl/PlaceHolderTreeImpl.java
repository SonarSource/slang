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
package org.sonarsource.slang.impl;

import java.util.Collections;
import java.util.List;
import org.sonarsource.slang.api.PlaceHolderTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

public class PlaceHolderTreeImpl extends IdentifierTreeImpl implements PlaceHolderTree {
  private final Token placeHolderToken;

  public PlaceHolderTreeImpl(TreeMetaData metaData, Token placeHolderToken) {
    super(metaData, "_");
    this.placeHolderToken = placeHolderToken;
  }

  @Override
  public Token placeHolderToken() {
    return placeHolderToken;
  }

  @Override
  public List<Tree> children() {
    return Collections.emptyList();
  }
}
