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
package org.sonarsource.ruby.converter;


import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.plugin.ParseException;

import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class RubyConverterTest {


  private static RubyConverter converter;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setUp() {
    converter = new RubyConverter();
  }

  @BeforeClass
  public static void tearDown() {
    converter.terminate();
  }

  @Test
  public void testParseException() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("(SyntaxError) unexpected token kEND");
    converter.parse("true\nend");
  }

  @Test
  public void testBasicAST() {
    Tree tree = converter.parse("require 'stuff'\n" +
      "a = 2 && 1");

    assertTree(tree).isNotNull();
  }

}