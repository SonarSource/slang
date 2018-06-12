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

import com.sonarsource.slang.api.ParameterTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ParameterTreeImpl extends BaseTreeImpl implements ParameterTree {

  private final String name;
  private final Tree type;

  public ParameterTreeImpl(TreeMetaData metaData, String name, @Nullable Tree type) {
    super(metaData);
    this.name = name;
    this.type = type;
  }

  @Override
  public String name() {
    return name;
  }

  @CheckForNull
  @Override
  public Tree type() {
    return type;
  }

  @Override
  public List<Tree> children() {
    return Collections.emptyList();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (obj instanceof ParameterTreeImpl) {
      ParameterTreeImpl other = (ParameterTreeImpl) obj;

      if (this.name.equals(other.name())) {
        if (this.type != null) {
          return this.type.equals(other.type());
        }

        return (other.type() == null);
      }
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }
}
