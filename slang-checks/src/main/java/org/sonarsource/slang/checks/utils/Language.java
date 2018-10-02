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
package org.sonarsource.slang.checks.utils;

/**
 * This enum is used only to distinguish default values for rule parameters. This should be the sole exception in otherwise
 * language agnostic module
 */
public enum Language {
  KOTLIN, RUBY, SCALA;

  public static final String RUBY_NAMING_DEFAULT = "^(@{0,2}[\\da-z_]+[!?=]?)|([*+-/%=!><~]+)|(\\[]=?)$";

  // support function name suffix '_=', '_+', '_!', ... and operators '+', '-', ...
  public static final String SCALA_FUNCTION_OR_OPERATOR_NAMING_DEFAULT = "^([a-z][a-zA-Z0-9]*+(_[^a-zA-Z0-9]++)?+|[^a-zA-Z0-9]++)$";

}
