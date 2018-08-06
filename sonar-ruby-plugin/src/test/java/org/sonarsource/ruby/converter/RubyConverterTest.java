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


import java.io.IOException;
import java.util.List;
import org.assertj.core.api.Condition;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.exceptions.StandardError;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.log.LogTester;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.plugin.ParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonarsource.slang.api.Token.Type.KEYWORD;
import static org.sonarsource.slang.api.Token.Type.OTHER;
import static org.sonarsource.slang.api.Token.Type.STRING_LITERAL;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class RubyConverterTest {

  private static RubyConverter converter;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

  @BeforeClass
  public static void setUp() {
    converter = new RubyConverter();
  }

  @BeforeClass
  public static void tearDown() {
    converter.terminate();
  }

  @Test
  public void exception() {
    Condition<ParseException> hasCorrectPosition = new Condition<>(e ->
      e.getPosition() != null && e.getPosition().line() == 2 && e.getPosition().lineOffset() == 0, "");
    assertThatThrownBy(() -> converter.parse("true\nend"))
      .isInstanceOf(ParseException.class)
      .hasMessage("(SyntaxError) unexpected token kEND")
      .matches(e -> hasCorrectPosition.matches((ParseException) e));
  }

  @Test
  public void error_location() {
    TextPointer errorLocation = converter.getErrorLocation(mock(StandardError.class));
    assertThat(errorLocation).isNull();
    assertThat(logTester.logs()).contains("No location information available for parse error");
  }

  @Test
  public void initialization_error() {
    RubyRuntimeAdapter mockedAdapter = mock(RubyRuntimeAdapter.class);
    when(mockedAdapter.eval(any(Ruby.class), any(String.class))).thenThrow(IOException.class);
    assertThatThrownBy(() -> new RubyConverter(mockedAdapter))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to initialized ruby runtime");
  }

  @Test
  public void empty() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("No AST node found");
    converter.parse("");
  }

  @Test
  public void top_level_tree() {
    assertTree(converter.parse(("true\nfalse"))).isInstanceOf(TopLevelTree.class);
    assertTree(converter.parse(("true\r\nfalse"))).isInstanceOf(TopLevelTree.class);

  }

  @Test
  public void ranges() {
    Tree tree = converter.parse("2; abc; 4\n2431323");
    assertTree(tree).hasTextRange(1, 0, 2, 7);

    tree = converter.parse("#start comment\n" +
      "require 'stuff'\n" +
      "a = 2 && 1\n" +
      "def method()\n" +
      "  a += 2 # line comment\n" +
      "end\n" +
      "result = obj.methodcall(argument) ; result");
    assertTree(tree).hasTextRange(1, 0, 7, 42);

    tree = converter.parse("# only comment file\n# comment line 2");
    assertTree(tree).hasTextRange(1, 0, 2, 16);
  }

  @Test
  public void comments() {
    TopLevelTree tree = (TopLevelTree) converter.parse("#start comment\n" +
      "require 'stuff'\n" +
      "a = 2 && 1\n" +
      "def method()\n" +
      "  a += 2 # line comment\n" +
      "end\n" +
      "result = obj.methodcall(argument) ; result\n" +
      "=begin\n" +
      "End multiline comment\n" +
      "=end\n");

    assertThat(tree.allComments()).extracting(Comment::text).containsExactly(
      "#start comment",
      "# line comment",
      "=begin\nEnd multiline comment\n=end\n");
    assertThat(tree.allComments()).extracting(Comment::contentText).containsExactly(
      "start comment",
      " line comment",
      "End multiline comment\n");
    assertRange(tree.allComments().get(0).textRange()).hasRange(1, 0, 1, 14);
    assertRange(tree.allComments().get(1).textRange()).hasRange(5, 9, 5, 23);
    assertRange(tree.allComments().get(2).textRange()).hasRange(8, 0, 11, 0);
    assertRange(tree.allComments().get(0).contentRange()).hasRange(1, 1, 1, 14);
    assertRange(tree.allComments().get(1).contentRange()).hasRange(5, 10, 5, 23);
    assertRange(tree.allComments().get(2).contentRange()).hasRange(9, 0, 9, 22);
  }

  @Test
  public void tokens() {
    Tree tree = converter.parse("# line comment\n" +
      "if a == 1\n" +
      "  a = \"ABC\"\n" +
      "end");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(8);
    assertThat(tokens).extracting(Token::text).containsExactly("if", "a", "==", "1", "a", "=", "ABC", "end");
    assertThat(tokens).extracting(Token::type).containsExactly(KEYWORD, OTHER, OTHER, OTHER, OTHER, OTHER, STRING_LITERAL, KEYWORD);
  }

}