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
package org.sonarsource.slang.impl;

import java.util.ArrayList;
import java.util.Collections;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.List;

public class TopLevelTreeImpl extends BaseTreeImpl implements TopLevelTree {

  private final List<Tree> preambleDeclarations;
  private final List<Tree> declarations;
  private final List<Comment> allComments;

  public TopLevelTreeImpl(TreeMetaData metaData, List<Tree> preambleDeclarations, List<Tree> declarations, List<Comment> allComments) {
    super(metaData);
    this.preambleDeclarations = preambleDeclarations;
    this.declarations = declarations;
    this.allComments = allComments;
  }

  public TopLevelTreeImpl(TreeMetaData metaData, List<Tree> declarations, List<Comment> allComments) {
    this(metaData, Collections.emptyList(), declarations, allComments);
  }

  @Override
  public List<Tree> preambleDeclarations() {
    return preambleDeclarations;
  }

  @Override
  public List<Tree> declarations() {
    return declarations;
  }

  @Override
  public List<Comment> allComments() {
    return allComments;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.addAll(preambleDeclarations());
    children.addAll(declarations());
    return children;
  }
}
