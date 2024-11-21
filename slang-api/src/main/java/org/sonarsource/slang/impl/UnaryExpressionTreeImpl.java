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

import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.api.UnaryExpressionTree;
import java.util.Collections;
import java.util.List;

public class UnaryExpressionTreeImpl extends BaseTreeImpl implements UnaryExpressionTree {

  private final Operator operator;
  private final Tree operand;

  public UnaryExpressionTreeImpl(TreeMetaData metaData, Operator operator, Tree operand) {
    super(metaData);
    this.operator = operator;
    this.operand = operand;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public Tree operand() {
    return operand;
  }

  @Override
  public List<Tree> children() {
    return Collections.singletonList(operand);
  }

}
