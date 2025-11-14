/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.checks.utils;

/**
 * This enum is used only to distinguish default values for rule parameters. This should be the sole exception in otherwise
 * language agnostic module
 */       
public enum Language {
  RUBY, SCALA, GO;

  public static final String RUBY_NAMING_DEFAULT = "^(@{0,2}[\\da-z_]+[!?=]?)|([*+-/%=!><~]+)|(\\[]=?)$";

  // scala constant starts with upper-case
  public static final String SCALA_NAMING_DEFAULT = "^[_a-zA-Z][a-zA-Z0-9]*$";

  // support function name suffix '_=', '_+', '_!', ... and operators '+', '-', ...
  public static final String SCALA_FUNCTION_OR_OPERATOR_NAMING_DEFAULT = "^([a-z][a-zA-Z0-9]*+(_[^a-zA-Z0-9]++)?+|[^a-zA-Z0-9]++)$";

  public static final String GO_NAMING_DEFAULT = "^(_|[a-zA-Z0-9]+)$";

  public static final int GO_NESTED_STATEMENT_MAX_DEPTH = 4;
  public static final int GO_MATCH_CASES_DEFAULT_MAX = 6;
  public static final int GO_DEFAULT_MAXIMUM_LINE_LENGTH = 120;
  public static final int GO_DEFAULT_FILE_LINE_MAX = 750;
  public static final int GO_DEFAULT_MAXIMUM_FUNCTION_LENGTH = 120;
}
