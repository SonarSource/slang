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
package org.sonarsource.slang.plugin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.TextPointerImpl;

public final class SlangTreeValidation {

  private SlangTreeValidation() {
    // utility
  }

  public static void validateTree(Tree tree, String code, Map<Class, Predicate<Token>> tokenValidationMap) {
    assertTokensMatchSourceCode(tree, code);
    assertTreeAcceptChildTokens(tree, tokenValidationMap);
  }

  private static void assertTokensMatchSourceCode(Tree tree, String code) {
    CodeFormToken reconstitutedCode = new CodeFormToken(tree.metaData().commentsInside());
    tree.metaData().tokens().forEach(reconstitutedCode::add);
    reconstitutedCode.addRemainingComments();
    reconstitutedCode.assertEqualTo(code);
  }

  private static void assertTreeAcceptChildTokens(Tree tree, Map<Class, Predicate<Token>> tokenValidationMap) {
    Set<Token> parentTokens = new HashSet<>(tree.metaData().tokens());
    for (Tree child : tree.children()) {
      removeChildTokens(parentTokens, child.metaData().tokens());
      assertTreeAcceptChildTokens(child, tokenValidationMap);
    }
    Predicate<Token> tokenPredicate = tokenValidationMap.get(tree.getClass());
    if (tokenPredicate != null) {
      parentTokens.removeIf(tokenPredicate::test);
    }
    if (!parentTokens.isEmpty()) {
      throwUnexpectedTokenException(tree, parentTokens);
    }
  }

  private static void removeChildTokens(Set<Token> parentTokens, List<Token> childTokens) {
    for (Token childToken : childTokens) {
      if (!parentTokens.remove(childToken)) {
        throw new ParseException("Token '" + childToken.text() + "' missing from parent tokens or already used by another child.",
          childToken.textRange().start());
      }
    }
  }

  private static void throwUnexpectedTokenException(Tree tree, Set<Token> unexpectedTokens) {
    String tokens = unexpectedTokens.stream()
      .sorted(Comparator.comparing(token -> token.textRange().start()))
      .map(Token::text)
      .collect(Collectors.joining("', '"));
    throw new ParseException("Token(s) '" + tokens + "' unexpected in " + tree.getClass().getSimpleName(), tree.textRange().start());
  }

  private static class CodeFormToken {
    private final StringBuilder code = new StringBuilder();
    private final List<Comment> commentsInside;
    private int lastLine = 1;
    private int lastLineOffset = 0;
    private int lastComment = 0;

    private CodeFormToken(List<Comment> commentsInside) {
      this.commentsInside = commentsInside;
    }

    private void add(Token token) {
      while (lastComment < commentsInside.size() &&
        commentsInside.get(lastComment).textRange().start().compareTo(token.textRange().start()) < 0) {
        Comment comment = commentsInside.get(lastComment);
        addTextAt(comment.text(), comment.textRange());
        lastComment++;
      }
      addTextAt(token.text(), token.textRange());
    }

    private void addRemainingComments() {
      for (int i = lastComment; i < commentsInside.size(); i++) {
        addTextAt(commentsInside.get(i).text(), commentsInside.get(i).textRange());
      }
    }

    private void addTextAt(String text, TextRange textRange) {
      while (lastLine < textRange.start().line()) {
        code.append("\n");
        lastLine++;
        lastLineOffset = 0;
      }
      while (lastLineOffset < textRange.start().lineOffset()) {
        code.append(' ');
        lastLineOffset++;
      }
      code.append(text);
      lastLine = textRange.end().line();
      lastLineOffset = textRange.end().lineOffset();
    }

    private void assertEqualTo(String expectedCode) {
      String[] actualLines = lines(this.code.toString());
      String[] expectedLines = lines(expectedCode);
      for (int i = 0; i < actualLines.length && i < expectedLines.length; i++) {
        if (!actualLines[i].equals(expectedLines[i])) {
          throw new ParseException("Unexpected AST difference at line: " + (i + 1) + "\n" +
            "Actual   : " + actualLines[i] + "\n" +
            "Expected : " + expectedLines[i] + "\n",
            new TextPointerImpl(i + 1, 0));
        }
      }
      if (actualLines.length != expectedLines.length) {
        throw new ParseException("Unexpected AST number of lines actual: " + actualLines.length + ", expected: " + expectedLines.length);
      }
    }

    private static String[] lines(String code) {
      return code
        .replace('\t', ' ')
        .replaceFirst("[\r\n ]+$", "")
        .split(" *(\r\n|\n|\r)", -1);
    }
  }

  public static class TokenValidationBuilder {

    private final Map<Class, Predicate<Token>> map = new HashMap<>();

    public TokenValidationBuilder patternFor(String regex, Class... classes) {
      Pattern pattern = Pattern.compile(regex);
      return acceptPredicateFor(token -> pattern.matcher(token.text()).matches(), classes);
    }

    public TokenValidationBuilder anyFor(Class... classes) {
      return acceptPredicateFor(token -> true, classes);
    }

    private TokenValidationBuilder acceptPredicateFor(Predicate<Token> predicate, Class... classes) {
      for (Class treeClass : classes) {
        map.put(treeClass, predicate);
      }
      return this;
    }

    public Map<Class, Predicate<Token>> build() {
      return new HashMap<>(map);
    }
  }

}
