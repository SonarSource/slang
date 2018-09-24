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
package org.sonarsource.scala.converter;

import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.TextPointerImpl;

public class ScalaConverter implements ASTConverter {

  @Override
  public Tree parse(String content) {
    throw new ParseException("Unable to parse file content.", new TextPointerImpl(1,1));
  }

}
