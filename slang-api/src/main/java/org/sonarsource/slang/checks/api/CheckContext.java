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
package org.sonarsource.slang.checks.api;

import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
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

  String filename();

  String fileContent();

  void reportIssue(TextRange textRange, String message);

  void reportIssue(HasTextRange toHighlight, String message);

  void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondaryLocation);

  void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations);

  void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap);

  void reportFileIssue(String message);

  void reportFileIssue(String message, @Nullable Double gap);
}
