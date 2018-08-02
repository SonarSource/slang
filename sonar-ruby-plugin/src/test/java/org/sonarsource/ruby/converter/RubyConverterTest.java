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


import java.util.List;
import org.jruby.RubySymbol;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonarsource.slang.parser.SLangConverter;
import org.sonarsource.slang.plugin.ParseException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.api.Token.Type.KEYWORD;
import static org.sonarsource.slang.api.Token.Type.OTHER;
import static org.sonarsource.slang.api.Token.Type.STRING_LITERAL;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.sonarsource.slang.testing.TreesAssert.assertTrees;

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
  public void exception() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("(SyntaxError) unexpected token kEND");
    converter.parse("true\nend");
  }

  @Test
  public void empty() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("No AST node found");
    converter.parse("");
  }

  @Test
  public void ast() {
    Tree tree = converter.parse("require 'stuff'\n" +
      "a = 2 && 1");

    Tree stringValue = nativeTree(nativeKind(String.class, "stuff"), emptyList());
    Tree stringLiteral = nativeTree(nativeKind(RubySymbol.class, "str"), singletonList(stringValue));
    Tree require = nativeTree(nativeKind(RubySymbol.class, "require"), emptyList());
    Tree requireCall = nativeTree(nativeKind(RubySymbol.class, "send"), asList(require, stringLiteral));

    Tree literal1 = literal("1");
    Tree literal2 = literal("2");
    Tree lit2AndLit1 = nativeTree(nativeKind(RubySymbol.class, "and"), asList(literal2, literal1));
    Tree identifierA = nativeTree(nativeKind(RubySymbol.class, "a"));
    Tree assignA = nativeTree(nativeKind(RubySymbol.class, "lvasgn"), asList(identifierA, lit2AndLit1));

    Tree expectedTree = new TopLevelTreeImpl(null, asList(requireCall, assignA), emptyList());
    assertTree(tree).isEquivalentTo(expectedTree);
  }

  @Test
  public void singletons() {
    assertTree(rubyStatement("true")).isEquivalentTo(nativeTree(nativeKind(RubySymbol.class, "true"), emptyList()));
    assertTree(rubyStatement("false")).isEquivalentTo(nativeTree(nativeKind(RubySymbol.class, "false"), emptyList()));
    assertTree(rubyStatement("nil")).isEquivalentTo(nativeTree(nativeKind(RubySymbol.class, "nil"), emptyList()));
  }

  @Test
  public void int_literals() {
    assertTrees(rubyStatements("2; 512; 4\n2431323"))
      .isEquivalentTo(slangStatements("2; 512; 4; 2431323;"));

    assertTree(rubyStatement("2")).isLiteral("2");
  }

  @Test
  public void ranges() {
    Tree tree = converter.parse("2; abc; 4\n2431323");
    assertTree(tree).hasTextRange(1, 0, 2, 7);
    assertTree(tree.children().get(0)).hasTextRange(1, 0, 1, 1);
    assertTree(tree.children().get(1)).hasTextRange(1, 3, 1, 6);

    tree = converter.parse("#start comment\n" +
      "require 'stuff'\n" +
      "a = 2 && 1\n" +
      "def method()\n" +
      "  a += 2 # line comment\n" +
      "end\n" +
      "result = obj.methodcall(argument) ; result");
    assertTree(tree).hasTextRange(1, 0, 7, 42);
    assertTree(tree.children().get(0)).hasTextRange(2, 0, 2, 15);
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
    assertRange(tree.allComments().get(2).contentRange()).hasRange(9, 0, 9, 21);

    assertThat(tree.children().get(0).metaData().commentsInside()).isEmpty(); // require call has no comment child
    assertThat(tree.children().get(2).metaData().commentsInside()).hasSize(1); // method has 1 comment child
  }

  @Test
  public void tokens() {
    Tree tree = converter.parse("# line comment\n" +
      "if a == 1\n" +
      "  a = \"ABC\"\n" +
      "end");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(9);
    assertThat(tokens).extracting(Token::text).containsExactly("# line comment", "if", "a", "==", "1", "a", "=", "ABC", "end");
    assertThat(tokens).extracting(Token::type).containsExactly(OTHER, KEYWORD, OTHER, OTHER, OTHER, OTHER, OTHER, STRING_LITERAL, KEYWORD);
  }

  private List<Tree> slangStatements(String innerCode) {
    Tree tree = new SLangConverter().parse(innerCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    return tree.children();
  }

  private Tree rubyStatement(String innerCode) {
    Tree tree = converter.parse(innerCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    assertThat(tree.children()).hasSize(1);
    return tree.children().get(0);
  }

  private List<Tree> rubyStatements(String innerCode) {
    Tree tree = converter.parse(innerCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    return tree.children();
  }

  public static LiteralTree literal(String value) {
    return new LiteralTreeImpl(null, value);
  }

  public static NativeTree nativeTree(NativeKind kind, List<Tree> children) {
    return new NativeTreeImpl(null, kind, children);
  }

  public static NativeTree nativeTree(NativeKind kind) {
    return new NativeTreeImpl(null, kind, emptyList());
  }

  public static NativeKind nativeKind(Class<?> rubyObjectClass, Object... differentiatorObjs) {
    return new RubyNativeKind(rubyObjectClass, differentiatorObjs);
  }

}