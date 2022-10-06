/*
 * SonarSource SLang
 * Copyright (C) 2018-2022 SonarSource SA
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
package org.sonarsource.slang.api;

import javax.annotation.Nullable;

public interface ASTConverter {

  /**
   * Use {@link ASTConverter#parse(String, String)} instead.
   * It provides improved logging when used with ASTConverterValidation.
   *
   * @deprecated
   * @since 1.8
   */
  @Deprecated
  Tree parse(String content);

  default Tree parse(String content, @Nullable String currentFile) {
    return parse(content);
  }

  default void terminate() {
    // Nothing to do by default
  }

}
