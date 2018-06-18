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
package com.sonarsource.slang.checks.api;

import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import java.util.Deque;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public interface CheckContext {

  Deque<Tree> ancestors();

  @CheckForNull
  default Tree parent() {
    if (this.ancestors().isEmpty()) {
      return null;
    } else {
      return this.ancestors().peek();
    }
  }

  void reportIssue(TextRange textRange, String message);

  void reportIssue(Tree tree, String message);

  void reportIssue(Tree tree, String message, SecondaryLocation secondaryLocation);

  void reportIssue(Tree tree, String message, List<SecondaryLocation> secondaryLocations);

  void reportIssue(Tree tree, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap);

}
