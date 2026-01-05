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
package org.sonarsource.slang.impl;

import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Collections;
import java.util.List;

public class ParenthesizedExpressionTreeImpl extends BaseTreeImpl implements ParenthesizedExpressionTree {

  private final Tree expression;
  private final Token leftParenthesis;
  private final Token rightParenthesis;

  public ParenthesizedExpressionTreeImpl(TreeMetaData metaData, Tree expression, Token leftParenthesis, Token rightParenthesis) {
    super(metaData);
    this.expression = expression;
    this.leftParenthesis = leftParenthesis;
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public Token leftParenthesis() {
    return leftParenthesis;
  }

  @Override
  public Token rightParenthesis() {
    return rightParenthesis;
  }

  @Override
  public List<Tree> children() {
    return Collections.singletonList(expression);
  }

}
