/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ClassDeclarationTreeImpl extends BaseTreeImpl implements ClassDeclarationTree {

  private final IdentifierTree identifier;
  private final Tree classTree;

  public ClassDeclarationTreeImpl(TreeMetaData metaData, @Nullable IdentifierTree identifier, Tree classTree) {
    super(metaData);
    this.identifier = identifier;
    this.classTree = classTree;
  }

  @CheckForNull
  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @Override
  public Tree classTree() {
    return classTree;
  }

  @Override
  public List<Tree> children() {
    // identifier is not added to the children as it is already part of this classTree structure
    return Collections.singletonList(classTree);
  }
}
