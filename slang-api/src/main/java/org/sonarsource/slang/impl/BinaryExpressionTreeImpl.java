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
package org.sonarsource.slang.impl;

import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Arrays;
import java.util.List;

public class BinaryExpressionTreeImpl extends BaseTreeImpl implements BinaryExpressionTree {

  private final Operator operator;
  private final Token operatorToken;
  private final Tree leftOperand;
  private final Tree rightOperand;

  public BinaryExpressionTreeImpl(TreeMetaData metaData, Operator operator, Token operatorToken, Tree leftOperand, Tree rightOperand) {
    super(metaData);
    this.operator = operator;
    this.operatorToken = operatorToken;

    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public Token operatorToken() {
    return operatorToken;
  }

  @Override
  public Tree leftOperand() {
    return leftOperand;
  }

  @Override
  public Tree rightOperand() {
    return rightOperand;
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(leftOperand, rightOperand);
  }
}
