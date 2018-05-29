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
