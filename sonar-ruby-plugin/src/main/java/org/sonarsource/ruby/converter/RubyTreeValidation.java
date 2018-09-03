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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import org.sonarsource.slang.impl.BaseTreeImpl;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.CatchTreeImpl;
import org.sonarsource.slang.impl.ClassDeclarationTreeImpl;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.ExceptionHandlingTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.IfTreeImpl;
import org.sonarsource.slang.impl.IntegerLiteralTreeImpl;
import org.sonarsource.slang.impl.JumpTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.LoopTreeImpl;
import org.sonarsource.slang.impl.MatchCaseTreeImpl;
import org.sonarsource.slang.impl.MatchTreeImpl;
import org.sonarsource.slang.impl.ModifierTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.ParameterTreeImpl;
import org.sonarsource.slang.impl.ParenthesizedExpressionTreeImpl;
import org.sonarsource.slang.impl.ReturnTreeImpl;
import org.sonarsource.slang.impl.StringLiteralTreeImpl;
import org.sonarsource.slang.impl.TextPointerImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.ThrowTreeImpl;
import org.sonarsource.slang.impl.TokenImpl;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonarsource.slang.impl.UnaryExpressionTreeImpl;
import org.sonarsource.slang.impl.VariableDeclarationTreeImpl;

import static java.util.regex.Pattern.compile;

public final class RubyTreeValidation {

  private static final Map<Class, Pattern> EXPECTED_TOKEN_MAP = buildExpectedTokenMap();

  private static final Set<String> KNOWN_TYPES_WITH_VALIDATION_ISSUES = new HashSet<>(Arrays.asList(
    "CatchTreeImpl/BlockTreeImpl",
    "ExceptionHandlingTreeImpl/BlockTreeImpl",
    "FunctionDeclarationTreeImpl/BlockTreeImpl",
    "IfTreeImpl/BlockTreeImpl",
    "MatchCaseTreeImpl/BlockTreeImpl",
    "NativeTreeImpl/IdentifierTreeImpl",
    "NativeTreeImpl/NativeTreeImpl",
    "NativeTreeImpl/ParameterTreeImpl"));

  private static Map<Class, Pattern> buildExpectedTokenMap() {
    Map<Class, Pattern> map = new HashMap<>();
    map.put(AssignmentExpressionTreeImpl.class, compile("[+\\-=]"));
    map.put(BaseTreeImpl.class, null);
    map.put(BinaryExpressionTreeImpl.class, compile("==|!=|<|>|<=|>=|&&|\\+|-|\\*|/|%|\\|\\||and|or"));
    map.put(BlockTreeImpl.class, compile("begin|end|#\\{|}|[;()]"));
    map.put(CatchTreeImpl.class, compile("rescue|else"));
    map.put(ClassDeclarationTreeImpl.class, null);
    map.put(CommentImpl.class, null);
    map.put(ExceptionHandlingTreeImpl.class, compile("begin|ensure|end"));
    map.put(FunctionDeclarationTreeImpl.class, compile("def|end|\\(|\\)|,|;|\\."));
    map.put(IdentifierTreeImpl.class, compile("[@$\\w_?]+"));
    map.put(IfTreeImpl.class, compile("if|elsif|else|end|;"));
    map.put(IntegerLiteralTreeImpl.class, compile("[0-9]+"));
    map.put(JumpTreeImpl.class, compile("next|break"));
    map.put(LiteralTreeImpl.class, compile("true|false"));
    map.put(LoopTreeImpl.class, compile("for|while|do|until|end|;"));
    map.put(MatchCaseTreeImpl.class, compile("when|then|else"));
    map.put(MatchTreeImpl.class, compile("case|end"));
    map.put(ModifierTreeImpl.class, null);
    map.put(NativeTreeImpl.class, compile("do|end|nil|class|else|in|raise|break|unless|next|self|CODE" +
      "|-|/|%|<|>|<<|>>|=|=>|&&|\\^|:|\\?|\"|<<\"|\\.\\.|\\||\\|\\||\\*|\\*\\*|[(),\\[\\]]"));
    map.put(ParameterTreeImpl.class, compile("[&*]+|arg"));
    map.put(ParenthesizedExpressionTreeImpl.class, compile("[()]"));
    map.put(ReturnTreeImpl.class, compile("return|,"));
    map.put(StringLiteralTreeImpl.class, compile(".*"));
    map.put(TextPointerImpl.class, null);
    map.put(TextRangeImpl.class, null);
    map.put(ThrowTreeImpl.class, compile("raise"));
    map.put(TokenImpl.class, null);
    map.put(TopLevelTreeImpl.class, compile(";"));
    map.put(UnaryExpressionTreeImpl.class, compile("[!~+\\-]|not"));
    map.put(VariableDeclarationTreeImpl.class, compile("="));
    return map;
  }

  private RubyTreeValidation() {
    // utility class
  }

  public static void validateCompleteness(Tree tree, @Nullable Tree parent) {
    String astType = parentAndNodeType(parent, tree);
    if (!KNOWN_TYPES_WITH_VALIDATION_ISSUES.contains(astType)) {
      Set<String> allTokens = tree.metaData().tokens().stream().map(RubyTreeValidation::asString).collect(Collectors.toSet());
      Set<String> unexpectedChildrenTokens = expectedChildrenTokens(tree);
      removeActualChildrenTokens(allTokens, unexpectedChildrenTokens, tree);
      if (!unexpectedChildrenTokens.isEmpty()) {
        String unexpectedTokenList = unexpectedTokenListOrdered(unexpectedChildrenTokens, tree);
        throw new IllegalStateException("Invalid Completeness for " + astType + ", missing: " + unexpectedTokenList);
      }
    }
    tree.children().forEach(child -> validateCompleteness(child, tree));
  }

  private static String parentAndNodeType(@Nullable Tree parent, Tree tree) {
    String astType = tree.getClass().getSimpleName();
    if (parent != null) {
      astType = parent.getClass().getSimpleName() + "/" + astType;
    }
    return astType;
  }

  private static Set<String> expectedChildrenTokens(Tree tree) {
    Pattern tokenAllowedPattern = EXPECTED_TOKEN_MAP.get(tree.getClass());
    return tree.metaData().tokens().stream()
      .filter(token -> tokenAllowedPattern == null || !tokenAllowedPattern.matcher(token.text()).matches())
      .map(RubyTreeValidation::asString)
      .collect(Collectors.toSet());
  }

  private static String unexpectedTokenListOrdered(Set<String> expectedChildrenTokens, Tree tree) {
    return tree.metaData().tokens().stream()
      .filter(token -> expectedChildrenTokens.contains(asString(token)))
      .map(RubyTreeValidation::asString)
      .collect(Collectors.joining(" "));
  }

  private static void removeActualChildrenTokens(Set<String> allTokens, Set<String> expectedChildrenTokens, Tree parent) {
    List<Tree> children = parent.children();
    for (Tree child : children) {
      String astType = parentAndNodeType(parent, child);
      child.metaData().tokens().stream()
        .map(RubyTreeValidation::asString)
        .forEach(token -> {
          expectedChildrenTokens.remove(token);
          if (!KNOWN_TYPES_WITH_VALIDATION_ISSUES.contains(astType) && !allTokens.remove(token)) {
            throw new IllegalStateException("Invalid Completeness for " + astType + ", extra token: " + token);
          }
        });
    }
  }

  private static String asString(Token token) {
    return "[" + token.textRange().start().line() + ":" + (token.textRange().start().lineOffset() + 1) + "]" + token.text();
  }

}
