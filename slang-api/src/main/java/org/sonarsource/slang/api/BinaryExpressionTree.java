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
package org.sonarsource.slang.api;

public interface BinaryExpressionTree extends Tree {

  public enum Operator {
    PLUS,
    MINUS,
    TIMES,
    DIVIDED_BY,

    EQUAL_TO,
    NOT_EQUAL_TO,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL_TO,
    LESS_THAN,
    LESS_THAN_OR_EQUAL_TO,

    CONDITIONAL_AND,
    CONDITIONAL_OR,
  }

  Operator operator();

  Token operatorToken();

  Tree leftOperand();

  Tree rightOperand();

}
