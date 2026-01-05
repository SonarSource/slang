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

import javax.annotation.CheckForNull;

/**
 * The interface used to define conditional expressions.
 *
 * In order to be compatible with most of the languages, the 'IfTree' is not defined 
 * as a statement and should be considered as an expression.
 *
 * A 'IfTree' always has:
 * - a keyword ('if', '?', 'unless', ...)
 * - a condition
 * - a 'then' branch 
 * 
 * Additionally, it's possible to also have:
 * - an 'else' keyword
 * - an 'else' branch (which does not necessarily requires a 'else' keyword)
 * 
 * Known mapping from languages conditional expressions to IfTree:
 * - Apex:   if, ternary (a?b:c)
 * - Ruby:   if, ternary (a?b:c), unless (equivalent to 'if not')
 * - Scala:  if
 */
public interface IfTree extends Tree {

  Tree condition();

  Tree thenBranch();

  @CheckForNull
  Tree elseBranch();

  Token ifKeyword();

  @CheckForNull
  Token elseKeyword();
}
