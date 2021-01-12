/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
import org.junit.Test;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.TextRanges;
import org.sonarsource.slang.impl.VariableDeclarationTreeImpl;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonarsource.slang.api.Token.Type.KEYWORD;
import static org.sonarsource.slang.api.Token.Type.OTHER;
import static org.sonarsource.slang.api.Token.Type.STRING_LITERAL;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;
import static org.sonarsource.slang.testing.TreesAssert.assertTrees;

public class RubyConverterTest extends AbstractRubyConverterTest {

  @Test
  public void syntax_exception() {
    Condition<ParseException> hasCorrectPosition = new Condition<>(e ->
      e.getPosition() != null && e.getPosition().line() == 2 && e.getPosition().lineOffset() == 0, "");
    assertThatThrownBy(() -> converter.parse("true\nend"))
      .isInstanceOf(ParseException.class)
      .hasMessage("(SyntaxError) unexpected token kEND")
      .matches(e -> hasCorrectPosition.matches((ParseException) e));
  }

  @Test
  public void parser_error() {
    RubyConverter rubyConverter = spy(new RubyConverter());
    doReturn(null).when(rubyConverter).invokeMethod(any(), any(), any());
    assertThatThrownBy(() -> rubyConverter.parse("true;"))
      .isInstanceOf(ParseException.class)
      .hasMessage("Unable to parse file content");
    rubyConverter.terminate();
  }

  @Test
  public void parser_runtime_exception() {
    RubyConverter rubyConverter = spy(new RubyConverter());
    doThrow(new RuntimeException("Runtime exception message")).when(rubyConverter).parseContent(any());
    assertThatThrownBy(() -> rubyConverter.parse(""))
      .isInstanceOf(ParseException.class)
      .hasMessage("Runtime exception message");
    rubyConverter.terminate();
  }

  @Test
  public void error_location_method_exception() {
    TextPointer errorLocation1 = converter.getErrorLocation(mock(StandardError.class));
    assertThat(errorLocation1).isNull();
    assertThat(logTester.logs()).contains("No location information available for parse error");
  }

  @Test
  public void error_location_null_result() {
    RubyConverter rubyConverter = spy(new RubyConverter());
    doReturn(null).when(rubyConverter).invokeMethod(any(), any(), any());
    TextPointer errorLocation2 = rubyConverter.getErrorLocation(mock(StandardError.class));
    assertThat(errorLocation2).isNull();
    assertThat(logTester.logs()).contains("No location information available for parse error");
    rubyConverter.terminate();
  }

  @Test
  public void initialization_error() {
    RubyRuntimeAdapter mockedAdapter = mock(RubyRuntimeAdapter.class);
    when(mockedAdapter.eval(any(Ruby.class), any(String.class))).thenAnswer(invocationOnMock -> {
      throw new IOException();
    });
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
  public void invalid_escape_sequence() {
    assertThat(converter.parse("\"\\xff\"")).isNotNull();
  }

  @Test
  public void ranges() {
    Tree topLevelTree = converter.parse("2; abc; 4\n2431323");
    assertTree(topLevelTree).hasTextRange(1, 0, 2, 7);
    Tree nativeAllInstructionsTree = topLevelTree.children().get(0);
    assertTree(nativeAllInstructionsTree.children().get(0)).hasTextRange(1, 0, 1, 1);
    assertTree(nativeAllInstructionsTree.children().get(1)).hasTextRange(1, 3, 1, 6);

    topLevelTree = converter.parse("#start comment\n" +
      "require 'stuff'\n" +
      "a = 2 && 1\n" +
      "def method()\n" +
      "  a += 2 # line comment\n" +
      "end\n" +
      "result = obj.methodcall(argument) ; result");
    nativeAllInstructionsTree = topLevelTree.children().get(0);
    assertTree(topLevelTree).hasTextRange(1, 0, 7, 42);
    assertTree(nativeAllInstructionsTree.children().get(0)).hasTextRange(2, 0, 2, 15);

    topLevelTree = converter.parse("# only comment file\n# comment line 2");
    assertTree(topLevelTree).hasTextRange(1, 0, 2, 16);
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
      "First line\n" +
      "End multiline comment\n" +
      "=end\n");

    assertThat(tree.allComments()).extracting(Comment::text).containsExactly(
      "#start comment",
      "# line comment",
      "=begin\nFirst line\nEnd multiline comment\n=end");
    assertThat(tree.allComments()).extracting(Comment::contentText).containsExactly(
      "start comment",
      " line comment",
      "First line\nEnd multiline comment");
    assertRange(tree.allComments().get(0).textRange()).hasRange(1, 0, 1, 14);
    assertRange(tree.allComments().get(1).textRange()).hasRange(5, 9, 5, 23);
    assertRange(tree.allComments().get(2).textRange()).hasRange(8, 0, 11, 4);
    assertRange(tree.allComments().get(0).contentRange()).hasRange(1, 1, 1, 14);
    assertRange(tree.allComments().get(1).contentRange()).hasRange(5, 10, 5, 23);
    assertRange(tree.allComments().get(2).contentRange()).hasRange(9, 0, 10, 21);
    assertThat(tree.children().get(0).children().get(0).metaData().commentsInside()).isEmpty(); // require call has no comment child
    assertThat(tree.children().get(0).children().get(2).metaData().commentsInside()).hasSize(1); // method has 1 comment child
  }

  @Test
  public void multiline_comments() throws Exception {
    assertComment("=begin\ncomment content\n=end\n", "=begin\ncomment content\n=end", "comment content",
      TextRanges.range(1, 0, 3, 4),
      TextRanges.range(2, 0, 2, 15));

    assertComment("=begin prefix \ncomment content\n=end",
      "=begin prefix \ncomment content\n=end",
      "prefix \ncomment content",
      TextRanges.range(1, 0, 3, 4),
      TextRanges.range(1, 7, 2, 15));

    assertComment("=begin \r\n comment \r\ncontent\r\n=end\r\n",
      "=begin \n" +
        " comment \n" +
        "content\n" +
        "=end",
      "comment \n" +
        "content",
      TextRanges.range(1, 0, 4, 4),
      TextRanges.range(2, 1, 3, 7));
  }

  @Test
  public void ast() {
    List<Tree> tree = rubyStatements("require 'stuff'\n" +
      "a = 2 && 1.0");
    Tree stringLiteral = stringLiteral("'stuff'", "stuff");
    Tree require = identifier("require");
    Tree requireCall = nativeTree(nativeKind("send"), asList(require, stringLiteral));
    Tree literal1 = nativeTree(nativeKind("float"), singletonList(nativeTree(nativeKind("1.0"), "1.0")));
    Tree literal2 = integerLiteral("2");
    Tree lit2AndLit1 = new BinaryExpressionTreeImpl(null, Operator.CONDITIONAL_AND, null, literal2, literal1);
    IdentifierTree identifierA = new IdentifierTreeImpl(null, "a");
    Tree assignA = new VariableDeclarationTreeImpl(null, identifierA, null, lit2AndLit1, false);
    assertTrees(tree).isEquivalentTo(asList(requireCall, assignA));
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
  
  // Since 2.6 version
  @Test
  public void yieldSelf() {
    Tree tree = converter.parse("class MyClass\n" +
      "  def some_method\n" +
      "    @path.yield_self(&File.method(:read)).yield_self(&Parser.method(:new)) ...\n" +
      "  end\n" +
      "end");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(30);
  }

  // Since 2.6 version
  @Test
  public void endless() {
    Tree tree = converter.parse("(1..).each {|index| }");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(11);
    assertThat(tokens).extracting(Token::text).containsExactly("(", "1", "..", ")", ".", "each", "{", "|", "index", "|", "}");
  }

  // Since 2.6 version
  @Test
  public void composition() {
    Tree tree = converter.parse("f = proc{|x| x + 2}\n" +
      "g = proc{|x| x * 3}\n" +
      "(f << g).call(3)\n" +
      "(f >> g).call(3)");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(42);
  }

  // Since 2.7 version
  @Test
  public void beginless() {
    Tree tree = converter.parse("ary[..3]\n" +
      "rel.where(sales: ..100)\n");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(13);
    assertThat(tokens).extracting(Token::text).containsExactly("ary", "[", "..", "3", "]", "rel", ".", "where", "(", "sales", "..", "100", ")");
  }

  private void assertComment(String input, String entireComment, String content, TextRange entireRange, TextRange contentRange) {
    TopLevelTree tree = (TopLevelTree) converter.parse(input);
    Comment comment = tree.allComments().get(0);
    assertThat(comment.text()).isEqualTo(entireComment);
    assertThat(comment.contentText()).isEqualTo(content);
    assertThat(comment.textRange()).isEqualTo(entireRange);
    assertThat(comment.contentRange()).isEqualTo(contentRange);
  }
  
  // Since 3.0 version
  
  @Test
  public void inReturnsFalse() {
    Tree tree = converter.parse("0 in 1");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(3);
    assertThat(tokens).extracting(Token::text).containsExactly("0", "in", "1");
  }

  @Test
  public void endlessMethodDef() {
    Tree tree = converter.parse("def square(x) = x * x");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(9);
    assertThat(tokens).extracting(Token::text).containsExactly("def", "square", "(", "x", ")", "=", "x", "*", "x");
  }
  
  @Test
  public void ractor() {
    Tree tree = converter.parse("def tarai(x, y, z) =\n" +
      "  x <= y ? y : tarai(tarai(x-1, y, z),\n" +
      "                     tarai(y-1, z, x),\n" +
      "                     tarai(z-1, x, y))\n" +
      "require 'benchmark'\n" +
      "Benchmark.bm do |x|\n" +
      "  # sequential version\n" +
      "  x.report('seq'){ 4.times{ tarai(14, 7, 0) } }\n" +
      "\n" +
      "  # parallel version\n" +
      "  x.report('par'){\n" +
      "    4.times.map do\n" +
      "      Ractor.new { tarai(14, 7, 0) }\n" +
      "    end.each(&:take)\n" +
      "  }\n" +
      "end");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(116);
  }
  
  @Test
  public void fiberScheduler() {
    Tree tree = converter.parse("require 'async'\n" +
      "require 'net/http'\n" +
      "require 'uri'\n" +
      "\n" +
      "Async do\n" +
      "  [\"ruby\", \"rails\", \"async\"].each do |topic|\n" +
      "    Async do\n" +
      "      Net::HTTP.get(URI \"https://www.google.com/search?q=#{topic}\")\n" +
      "    end\n" +
      "  end\n" +
      "end");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(40);
  }
  
  @Test
  public void onelinePatternMatching() {
    Tree tree = converter.parse("0 => a\n" +
      "p a #=> 0\n" +
      "\n" +
      "{b: 0, c: 1} => {b:}\n" +
      "p b #=> 0");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(18);
  }
  
  @Test
  public void findPattern() {
    Tree tree = converter.parse("case [\"a\", 1, \"b\", \"c\", 2, \"d\", \"e\", \"f\", 3]\n" +
      "in [*pre, String => x, String => y, *post]\n" +
      "  p pre  #=> [\"a\", 1]\n" +
      "  p x    #=> \"b\"\n" +
      "  p y    #=> \"c\"\n" +
      "  p post #=> [2, \"d\", \"e\", \"f\", 3]\n" +
      "end");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(45);
  }

  @Test
  public void hashExcept() {
    Tree tree = converter.parse("h = { a: 1, b: 2, c: 3 }\n" +
      "p h.except(:a) #=> {:b=>2, :c=>3}");

    List<Token> tokens = tree.metaData().tokens();

    assertThat(tokens).hasSize(19);
  }

}
