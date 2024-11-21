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

import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ReturnTreeImpl extends BaseTreeImpl implements ReturnTree {
  private final Tree body;
  private final Token keyword;

  public ReturnTreeImpl(TreeMetaData metaData, Token keyword, @Nullable Tree body) {
    super(metaData);
    this.body = body;
    this.keyword = keyword;
  }

  @CheckForNull
  @Override
  public Tree body() {
    return body;
  }

  @Override
  public Token keyword() {
    return keyword;
  }

  @Override
  public List<Tree> children() {
    return body == null ? Collections.emptyList() : Collections.singletonList(body);
  }
}
