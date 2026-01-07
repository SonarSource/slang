/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
package org.sonarsource.slang.api;

import javax.annotation.Nullable;

public interface ASTConverter {

  /**
   * @deprecated
   * Use {@link ASTConverter#parse(String, String)} instead.
   * It provides improved logging when used with ASTConverterValidation.
   */
  @Deprecated(since = "1.8")
  Tree parse(String content);

  default Tree parse(String content, @Nullable String currentFile) {
    return parse(content);
  }

  default void terminate() {
    // Nothing to do by default
  }

}
