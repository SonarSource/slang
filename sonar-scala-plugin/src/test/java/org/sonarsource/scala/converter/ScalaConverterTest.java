/*
 * SonarSource SLang
 * Copyright (C) 2018-2020 SonarSource SA
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
import org.sonarsource.slang.api.ImportDeclarationTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.PackageDeclarationTree;
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

public class ScalaConverterTest extends AbstractScalaConverterTest {

  @Test
  public void parser_error() {
    assertThatThrownBy(() -> parse("object Main {..."))
      .isInstanceOf(ParseException.class)
      .hasMessage("Unable to parse file content.")
      .matches(e -> {
        TextPointer position = ((ParseException) e).getPosition();
        return position.line() == 1 && position.lineOffset() == 13;
      });
  }

  @Test
  public void top_level_tree() {
    Tree tree = parse("object Main { print(\"Hello!\") }");
    assertThat(tree).isInstanceOf(TopLevelTree.class);
  }

  @Test
  public void empty_top_level_tree() {
    assertThat(parse("")).isInstanceOf(TopLevelTree.class);
    assertThat(parse("  \n")).isInstanceOf(TopLevelTree.class);
  }

  @Test
  public void package_and_import_declarations() {
    Tree tree = parse("package abc\nimport x.y\nobject MyObj{}");
    Tree pkg = tree.children().get(0);
    assertThat(pkg).isInstanceOf(PackageDeclarationTree.class);
    assertThat(pkg.children()).hasSize(3);
    assertThat(pkg.children().get(1)).isInstanceOf(ImportDeclarationTree.class);
  }

  @Test
  public void tokens() {
    Tree tree = parse("object Main /* comment */ {\n" +
      "  print(\"hello world!\")\n" +
      "\tprint(s\"$hello $world!\")\r\n" +
      "}\n");
    List<Token> tokens = tree.metaData().tokens();
    assertThat(tokens).extracting(Token::text).containsExactly(
      "object", "Main", "{",
      "print", "(", "\"hello world!\"", ")",
      "print", "(", "s", "\"", "$", "hello", " ", "$", "world", "!", "\"", ")",
      "}");
    assertThat(tokens).extracting(Token::type).containsExactly(
      KEYWORD, OTHER, OTHER,
      OTHER, OTHER, STRING_LITERAL, OTHER,
      OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER, OTHER,
      OTHER);
    assertRange(tokens.get(1).textRange()).hasRange(1, 7, 1, 11);
  }

  @Test
  public void comments() {
    Tree tree = parse("object Main /* multi\n line */ {\nprint(\"Hello!\") //inline\n }");
    List<Comment> comments = tree.metaData().commentsInside();
    assertThat(comments).extracting(Comment::text).containsExactly("/* multi\n line */", "//inline");
    assertThat(comments).extracting(Comment::contentText).containsExactly(" multi\n line ", "inline");
    assertRange(comments.get(0).textRange()).hasRange(1, 12, 2, 8);
    assertRange(comments.get(0).contentRange()).hasRange(1, 14, 2, 6);
    assertRange(comments.get(1).textRange()).hasRange(3, 16, 3, 24);
    assertRange(comments.get(1).contentRange()).hasRange(3, 18, 3, 24);
  }

  @Test
  public void matchTreeCase_textRange_correction() {
    Tree tree = parse("class MyClass {\n" +
      "  def myMethod(cond: Any) = {\n" +
      "    cond match {\n" +
      "      case 1 =>\n" +
      "        val x: Int = 1 + 1\n" +
      "        x\n" +
      "      case _ =>\n" +
      "        1\n" +
      "    }\n" +
      "  }\n" +
      "}");
    assertRange(tree.metaData().textRange()).hasRange(1, 0, 11, 1);
    Tree matchTree = tree.descendants().filter(t -> t instanceof MatchTree).findFirst().get();
    Tree firstMatchCase = tree.descendants().filter(t -> t instanceof MatchCaseTree).findFirst().get();
    Tree defaultMatchCase = tree.descendants().filter(t -> t instanceof MatchCaseTree).skip(1).findFirst().get();
    assertRange(firstMatchCase.metaData().textRange()).hasRange(4,6,7,0);
    assertRange(defaultMatchCase.metaData().textRange()).hasRange(7,6,8,9);
    // note that the parent 'matchTree' ends when the last child ends (not on the next line, with the ending curly brace token)
    // normally it should end at line 9 column 5
    assertRange(matchTree.metaData().textRange()).hasRange(3,4,8,9);
  }

  @Test
  public void empty_scalameta_literal_node_in_if_without_else() {
    Tree tree = scalaStatement("if (x) { y }");
    assertThat(tree.descendants().filter(t -> t instanceof LiteralTree)).isEmpty();
  }

  @Test
  public void first_cpd_token() {
    TopLevelTree topLevel = (TopLevelTree) parse("" +
      "package com.example\n" +
      "import com.example.MyClass\n" +
      "object Obj {}");
    assertThat(topLevel.firstCpdToken().text()).isEqualTo("object");
  }
}
