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

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

public class TopLevelTreeImpl extends BaseTreeImpl implements TopLevelTree {

  private final List<Tree> declarations;
  private final List<Comment> allComments;
  private final Token firstCpdToken;

  public TopLevelTreeImpl(TreeMetaData metaData, List<Tree> declarations, List<Comment> allComments) {
    this(metaData, declarations, allComments, null);
  }

  public TopLevelTreeImpl(TreeMetaData metaData, List<Tree> declarations, List<Comment> allComments, @Nullable Token firstCpdToken) {
    super(metaData);
    this.declarations = declarations;
    this.allComments = allComments;
    this.firstCpdToken = firstCpdToken;
  }

  @Override
  public List<Tree> declarations() {
    return declarations;
  }

  @Override
  public List<Comment> allComments() {
    return allComments;
  }

  @CheckForNull
  @Override
  public Token firstCpdToken() {
    return firstCpdToken;
  }

  @Override
  public List<Tree> children() {
    return declarations();
  }
}
