/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.slang.impl;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

public class MatchTreeImpl extends BaseTreeImpl implements MatchTree {

  private final Tree expression;
  private final List<MatchCaseTree> cases;
  private final Token keyword;

  public MatchTreeImpl(TreeMetaData metaData, @Nullable Tree expression, List<MatchCaseTree> cases, Token keyword) {
    super(metaData);
    this.expression = expression;
    this.cases = cases;
    this.keyword = keyword;
  }

  @CheckForNull
  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public List<MatchCaseTree> cases() {
    return cases;
  }

  @Override
  public Token keyword() {
    return keyword;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    if (expression != null) {
      children.add(expression);
    }
    children.addAll(cases);
    return children;
  }
}
