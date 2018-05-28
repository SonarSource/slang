package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.Tree;

public class BinaryExpressionTreeImpl implements BinaryExpressionTree {

  private final Operator operator;
  private final Tree leftOperand;
  private final Tree rightOperand;

  public BinaryExpressionTreeImpl(Operator operator, Tree leftOperand, Tree rightOperand) {
    this.operator = operator;
    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public Tree leftOperand() {
    return leftOperand;
  }

  @Override
  public Tree rightOperand() {
    return rightOperand;
  }
}
