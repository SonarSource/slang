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

import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ParameterTreeImpl extends BaseTreeImpl implements ParameterTree {

  private final IdentifierTree identifier;
  private final Tree type;

  public ParameterTreeImpl(TreeMetaData metaData, IdentifierTree identifier, @Nullable Tree type) {
    super(metaData);
    this.identifier = identifier;
    this.type = type;
  }

  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @CheckForNull
  @Override
  public Tree type() {
    return type;
  }

  @Override
  public List<Tree> children() {
    if (type != null) {
      return Arrays.asList(identifier, type);
    } else {
      return Collections.singletonList(identifier);
    }
  }

}
