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

import java.util.List;
import org.junit.Test;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonarsource.slang.api.Token.Type.KEYWORD;
import static org.sonarsource.slang.api.Token.Type.OTHER;
import static org.sonarsource.slang.api.Token.Type.STRING_LITERAL;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;

public class ScalaConverterTest {

  private final ScalaConverter converter = new ScalaConverter();

  @Test
  public void parser_error() {
    assertThatThrownBy(() -> converter.parse("object Main {..."))
      .isInstanceOf(ParseException.class)
      .hasMessage("Unable to parse file content.")
      .matches(e -> {
        TextPointer position = ((ParseException) e).getPosition();
        return position.line() == 1 && position.lineOffset() == 13;
      });
  }

  @Test
  public void top_level_tree() {
    Tree tree = converter.parse("object Main { print(\"Hello!\") }");
    assertThat(tree).isInstanceOf(TopLevelTree.class);
  }

  @Test
  public void tokens() {
    Tree tree = converter.parse("object Main /* comment */ { print(\"Hello!\") }");
    List<Token> tokens = tree.metaData().tokens();
    assertThat(tokens).extracting(Token::text).containsExactly("object", "Main", "{", "print", "(", "\"Hello!\"", ")", "}");
    assertThat(tokens).extracting(Token::type).containsExactly(KEYWORD, OTHER, OTHER, OTHER, OTHER, STRING_LITERAL, OTHER, OTHER);
    assertRange(tokens.get(1).textRange()).hasRange(1, 7, 1, 11);
  }

  @Test
  public void comments() {
    Tree tree = converter.parse("object Main /* multi\n line */ {\nprint(\"Hello!\") //inline\n }");
    List<Comment> comments = tree.metaData().commentsInside();
    assertThat(comments).extracting(Comment::text).containsExactly("/* multi\n line */", "//inline");
    assertThat(comments).extracting(Comment::contentText).containsExactly(" multi\n line ", "inline");
    assertRange(comments.get(0).textRange()).hasRange(1, 12, 2, 8);
    assertRange(comments.get(0).contentRange()).hasRange(1, 14, 2, 6);
    assertRange(comments.get(1).textRange()).hasRange(3, 16, 3, 24);
    assertRange(comments.get(1).contentRange()).hasRange(3, 18, 3, 24);
  }

}
