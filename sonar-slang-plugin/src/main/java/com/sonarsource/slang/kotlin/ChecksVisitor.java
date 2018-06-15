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
package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SecondaryLocation;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;

public class ChecksVisitor extends TreeVisitor<InputFileContext> {

  public ChecksVisitor(Checks<SlangCheck> checks) {
    Collection<SlangCheck> rulesActiveInSonarQube = checks.all();
    for (SlangCheck check : rulesActiveInSonarQube) {
      RuleKey ruleKey = checks.ruleKey(check);
      Objects.requireNonNull(ruleKey);
      check.initialize(new ContextAdapter(ruleKey));
    }
  }

  public class ContextAdapter implements InitContext, CheckContext {

    public final RuleKey ruleKey;
    private InputFileContext currentCtx;

    public ContextAdapter(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
    }

    @Override
    public <T extends Tree> void register(Class<T> cls, BiConsumer<CheckContext, T> visitor) {
      ChecksVisitor.this.register(cls, (ctx, tree) -> {
        this.currentCtx = ctx;
        visitor.accept(this, tree);
      });
    }

    @Override
    public Deque<Tree> ancestors() {
      return currentCtx.ancestors();
    }

    @Override
    public void reportIssue(TextRange textRange, String message) {
      reportIssue(textRange, message, Collections.emptyList(), null);
    }

    @Override
    public void reportIssue(Tree tree, String message) {
      reportIssue(tree, message, Collections.emptyList());
    }

    @Override
    public void reportIssue(Tree tree, String message, SecondaryLocation secondaryLocation) {
      reportIssue(tree, message, Collections.singletonList(secondaryLocation));
    }

    @Override
    public void reportIssue(Tree tree, String message, List<SecondaryLocation> secondaryLocations) {
      reportIssue(tree, message, secondaryLocations, null);
    }

    @Override
    public void reportIssue(Tree tree, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      reportIssue(tree.metaData().textRange(), message, secondaryLocations, gap);
    }

    private void reportIssue(TextRange textRange, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      currentCtx.reportIssue(ruleKey, textRange, message, secondaryLocations, gap);
    }

  }

}
