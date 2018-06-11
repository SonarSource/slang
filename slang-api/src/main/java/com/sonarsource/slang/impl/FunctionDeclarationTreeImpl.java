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

import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class FunctionDeclarationTreeImpl extends BaseTreeImpl implements FunctionDeclarationTree {

  private final List<Tree> modifiers;
  private final Tree returnType;
  private final IdentifierTree name;
  private final List<IdentifierTree> formalParameters;
  private final BlockTree body;
  private final List<Tree> children = new ArrayList<>();

  public FunctionDeclarationTreeImpl(
    TreeMetaData metaData,
    List<Tree> modifiers,
    @Nullable Tree returnType,
    @Nullable IdentifierTree name,
    List<IdentifierTree> formalParameters,
    @Nullable BlockTree body
  ) {
    super(metaData);

    this.modifiers = modifiers;
    this.returnType = returnType;
    this.name = name;
    this.formalParameters = formalParameters;
    this.body = body;

    this.children.addAll(modifiers);
    if (returnType != null) {
      this.children.add(returnType);
    }
    if (name != null) {
      this.children.add(name);
    }
    this.children.addAll(formalParameters);
    if (body != null) {
      this.children.add(body);
    }
  }

  @Override
  public List<Tree> modifiers() {
    return modifiers;
  }

  @CheckForNull
  @Override
  public Tree returnType() {
    return returnType;
  }

  @CheckForNull
  @Override
  public IdentifierTree name() {
    return name;
  }

  @Override
  public List<IdentifierTree> formalParameters() {
    return formalParameters;
  }

  @CheckForNull
  @Override
  public BlockTree body() {
    return body;
  }

  @Override
  public List<Tree> children() {
    return children;
  }
}
