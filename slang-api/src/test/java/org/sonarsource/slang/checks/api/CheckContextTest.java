/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
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

import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.visitors.TreeContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CheckContextTest {

  @Test
  void parent_default_method() {
    Tree a = mock(Tree.class);
    Tree b = mock(Tree.class);
    Tree c = mock(Tree.class);

    CheckContextToTestDefaultMethod context = new CheckContextToTestDefaultMethod();
    assertThat(context.parent()).isNull();

    context.enter(a);
    assertThat(context.parent()).isNull();

    context.enter(b);
    assertThat(context.parent()).isSameAs(a);

    context.enter(c);
    assertThat(context.parent()).isSameAs(b);

    context.leave(c);
    assertThat(context.parent()).isSameAs(a);

    context.leave(b);
    assertThat(context.parent()).isNull();
  }

  private static class CheckContextToTestDefaultMethod extends TreeContext implements CheckContext {

    public String filename() {
      return null;
    }

    public String fileContent() {
      return null;
    }

    public void reportIssue(TextRange textRange, String message) {
    }

    public void reportIssue(HasTextRange toHighlight, String message) {
    }

    public void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondaryLocation) {
    }

    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations) {
    }

    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
    }

    public void reportFileIssue(String message) {
    }

    public void reportFileIssue(String message, @Nullable Double gap) {
    }

  }

}
