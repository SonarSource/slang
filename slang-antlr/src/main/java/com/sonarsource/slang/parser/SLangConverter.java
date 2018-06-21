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
package com.sonarsource.slang.parser;

import com.sonarsource.slang.api.ASTConverter;
import com.sonarsource.slang.api.AssignmentExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.CatchTree;
import com.sonarsource.slang.api.Comment;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.ParameterTree;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Token.Type;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import com.sonarsource.slang.api.UnaryExpressionTree;
import com.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.BlockTreeImpl;
import com.sonarsource.slang.impl.CatchTreeImpl;
import com.sonarsource.slang.impl.CommentImpl;
import com.sonarsource.slang.impl.ExceptionHandlingTreeImpl;
import com.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.IfTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.MatchCaseTreeImpl;
import com.sonarsource.slang.impl.MatchTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import com.sonarsource.slang.impl.ParameterTreeImpl;
import com.sonarsource.slang.impl.StringLiteralTreeImpl;
import com.sonarsource.slang.impl.TextPointerImpl;
import com.sonarsource.slang.impl.TextRangeImpl;
import com.sonarsource.slang.impl.TokenImpl;
import com.sonarsource.slang.impl.TopLevelTreeImpl;
import com.sonarsource.slang.impl.TreeMetaDataProvider;
import com.sonarsource.slang.impl.UnaryExpressionTreeImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonarsource.slang.impl.VariableDeclarationTreeImpl;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sonarsource.analyzer.commons.TokenLocation;

import static java.util.stream.Collectors.toList;

public class SLangConverter implements ASTConverter {

  private static final Set<Integer> KEYWORD_TOKEN_TYPES = new HashSet<>(Arrays.asList(
    SLangParser.ELSE,
    SLangParser.FUN,
    SLangParser.IF,
    SLangParser.MATCH,
    SLangParser.NATIVE,
    SLangParser.PRIVATE,
    SLangParser.PUBLIC,
    SLangParser.RETURN,
    SLangParser.THIS
  ));

  @Override
  public Tree parse(String slangCode) {
    SLangLexer lexer = new SLangLexer(CharStreams.fromString(slangCode));

    List<Comment> comments = new ArrayList<>();
    CommonTokenStream antlrTokens = new CommonTokenStream(lexer);
    antlrTokens.fill();

    List<com.sonarsource.slang.api.Token> tokens = new ArrayList<>();

    for (int index = 0; index < antlrTokens.size(); index++) {
      Token token = antlrTokens.get(index);
      TextRange textRange = getSlangTextRange(token);
      if (token.getChannel() == 1) {
        comments.add(new CommentImpl(commentContent(token.getText()), token.getText(), textRange));
      } else {
        Type type = Type.OTHER;
        if (KEYWORD_TOKEN_TYPES.contains(token.getType())) {
          type = Type.KEYWORD;
        } else if (token.getType() == SLangParser.StringLiteral) {
          type = Type.STRING_LITERAL;
        }
        tokens.add(new TokenImpl(textRange, token.getText(), type));
      }
    }

    SLangParser parser = new SLangParser(antlrTokens);

    SLangParseTreeVisitor slangVisitor = new SLangParseTreeVisitor(comments, tokens);
    return slangVisitor.visit(parser.slangFile());
  }

  private static String commentContent(String text) {
    if (text.startsWith("//")) {
      return text.substring(2);
    }
    return text.substring(2, text.length() - 2);
  }

  private static final Map<String, Operator> BINARY_OPERATOR_MAP = binaryOperatorMap();
  private static final Map<String, AssignmentExpressionTree.Operator> ASSIGNMENT_OPERATOR_MAP = assignmentOperatorMap();

  private static Map<String, Operator> binaryOperatorMap() {
    Map<String, Operator> map = new HashMap<>();
    map.put(">", Operator.GREATER_THAN);
    map.put(">=", Operator.GREATER_THAN_OR_EQUAL_TO);
    map.put("<", Operator.LESS_THAN);
    map.put("<=", Operator.LESS_THAN_OR_EQUAL_TO);
    map.put("==", Operator.EQUAL_TO);
    map.put("!=", Operator.NOT_EQUAL_TO);
    map.put("+", Operator.PLUS);
    map.put("-", Operator.MINUS);
    map.put("*", Operator.TIMES);
    map.put("/", Operator.DIVIDED_BY);
    return Collections.unmodifiableMap(map);
  }

  private static Map<String, AssignmentExpressionTree.Operator> assignmentOperatorMap() {
    Map<String, AssignmentExpressionTree.Operator> map = new HashMap<>();
    map.put("=", AssignmentExpressionTree.Operator.EQUAL);
    map.put("+=", AssignmentExpressionTree.Operator.PLUS_EQUAL);
    map.put("-=", AssignmentExpressionTree.Operator.MINUS_EQUAL);
    map.put("*=", AssignmentExpressionTree.Operator.TIMES_EQUAL);
    map.put("%=", AssignmentExpressionTree.Operator.MODULO_EQUAL);
    return Collections.unmodifiableMap(map);
  }

  private static TextRange getSlangTextRange(Token matchToken) {
    TokenLocation location = new TokenLocation(matchToken.getLine(), matchToken.getCharPositionInLine(), matchToken.getText());
    return new TextRangeImpl(location.startLine(), location.startLineOffset(), location.endLine(), location.endLineOffset());
  }

  private static class SLangParseTreeVisitor extends SLangBaseVisitor<Tree> {

    private final TreeMetaDataProvider metaDataProvider;

    public SLangParseTreeVisitor(List<Comment> comments, List<com.sonarsource.slang.api.Token> tokens) {
      metaDataProvider = new TreeMetaDataProvider(comments, tokens);
    }

    @Override
    public Tree visitSlangFile(SLangParser.SlangFileContext ctx) {
      // Special case for text range here, as last token is <EOF> which has length 5, so we only go up to the start of the <EOF> token
      TextRangeImpl textRange = new TextRangeImpl(startOf(ctx.start), new TextPointerImpl(ctx.stop.getLine(), ctx.stop.getCharPositionInLine()));
      return new TopLevelTreeImpl(meta(textRange), list(ctx.typeDeclaration()), metaDataProvider.allComments());
    }

    @Override
    public Tree visitTypeDeclaration(SLangParser.TypeDeclarationContext ctx) {
      if (ctx.methodDeclaration() != null) {
        return visit(ctx.methodDeclaration());
      } else {
        return visit(ctx.statement());
      }
     }

    @Override
    public Tree visitNativeExpression(SLangParser.NativeExpressionContext ctx) {
      return nativeTree(ctx, ctx.nativeBlock());
    }

    @Override
    public Tree visitParenthesizedExpression(SLangParser.ParenthesizedExpressionContext ctx) {
      return visit(ctx.statement());
    }

    @Override
    public Tree visitMethodDeclaration(SLangParser.MethodDeclarationContext ctx) {
      List<Tree> modifiers = list(ctx.methodModifier());
      Tree returnType = null;
      IdentifierTree name = null;
      SLangParser.MethodHeaderContext methodHeaderContext = ctx.methodHeader();
      SLangParser.SimpleTypeContext resultContext = methodHeaderContext.simpleType();
      SLangParser.IdentifierContext identifier = methodHeaderContext.methodDeclarator().identifier();
      if (resultContext != null) {
        returnType = new IdentifierTreeImpl(meta(resultContext), resultContext.getText());
      }
      if (identifier != null) {
        name = (IdentifierTree) visit(identifier);
      }

      List<ParameterTree> convertedParameters = new ArrayList<>();
      SLangParser.FormalParameterListContext formalParameterListContext = methodHeaderContext.methodDeclarator().formalParameterList();
      if (formalParameterListContext != null) {
        SLangParser.FormalParametersContext formalParameters = formalParameterListContext.formalParameters();
        if (formalParameters != null) {
          convertedParameters.addAll(list(formalParameters.formalParameter()).stream().map(ParameterTree.class::cast).collect(toList()));
        }
        convertedParameters.add((ParameterTree) visit(formalParameterListContext.lastFormalParameter()));
      }

      return new FunctionDeclarationTreeImpl(meta(ctx), modifiers, returnType, name, convertedParameters, (BlockTree) visit(ctx.methodBody()));
    }

    @Override
    public Tree visitMethodModifier(SLangParser.MethodModifierContext ctx) {
      return simpleNativeTree(ctx);
    }

    @Override
    public Tree visitMethodBody(SLangParser.MethodBodyContext ctx) {
      if (ctx.SEMICOLON() != null) {
        return null;
      }
      return visit(ctx.block());
    }

    @Override
    public Tree visitFormalParameter(SLangParser.FormalParameterContext ctx) {
      IdentifierTree tree = (IdentifierTree) visit(ctx.variableDeclaratorId().identifier());
      Tree type = null;
      if (ctx.simpleType() != null) {
        type = new IdentifierTreeImpl(meta(ctx.simpleType()), ctx.simpleType().getText());
      }
      return new ParameterTreeImpl(meta(ctx), tree, type);
    }

    @Override
    public Tree visitLastFormalParameter(SLangParser.LastFormalParameterContext ctx) {
      return visit(ctx.formalParameter());
    }

    @Override
    public Tree visitDeclaration(SLangParser.DeclarationContext ctx) {
      IdentifierTree identifier = (IdentifierTree) visit(ctx.identifier());
      Tree type = null;
      if (ctx.simpleType() != null) {
        type = new IdentifierTreeImpl(meta(ctx.simpleType()), ctx.simpleType().getText());
      }
      Tree initializer = null;
      if (ctx.expression() != null) {
        initializer = visit(ctx.expression());
      }

      boolean isVal = ctx.declarationModifier().VAL() != null;
      return new VariableDeclarationTreeImpl(meta(ctx), identifier, type, initializer, isVal);
    }

    @Override
    public Tree visitBlock(SLangParser.BlockContext ctx) {
      return new BlockTreeImpl(meta(ctx), list(ctx.statement()));
    }

    @Override
    public Tree visitIfExpression(SLangParser.IfExpressionContext ctx) {
      Tree elseBranch = null;
      if (ctx.controlBlock().size() > 1) {
        elseBranch = visit(ctx.controlBlock(1));
      }
      Tree thenBranch = visit(ctx.controlBlock(0));
      return new IfTreeImpl(meta(ctx), visit(ctx.statement()), thenBranch, elseBranch);
    }

    @Override
    public Tree visitMatchExpression(SLangParser.MatchExpressionContext ctx) {
      List<MatchCaseTree> cases = new ArrayList<>();
      for (SLangParser.MatchCaseContext matchCaseContext : ctx.matchCase()) {
        cases.add((MatchCaseTree) visit(matchCaseContext));
      }
      TreeMetaData meta = meta(ctx);
      return new MatchTreeImpl(
        meta,
        visit(ctx.statement()),
        cases,
        getMatchKeyword(ctx.MATCH().getSymbol()));
    }

    private static com.sonarsource.slang.api.Token getMatchKeyword(Token matchToken) {
      TextRange textRange = getSlangTextRange(matchToken);
      return new TokenImpl(textRange, matchToken.getText(), Type.KEYWORD);
    }

    @Override
    public Tree visitMatchCase(SLangParser.MatchCaseContext ctx) {
      Tree expression = ctx.statement() == null ? null : visit(ctx.statement());
      Tree body = visit(ctx.controlBlock());
      return new MatchCaseTreeImpl(meta(ctx), expression, body);
    }

    @Override
    public Tree visitCatchBlock(SLangParser.CatchBlockContext ctx) {
      ParameterTree parameter = ctx.formalParameter() == null ? null : (ParameterTree) visit(ctx.formalParameter());
      Tree body = visit(ctx.block());
      return new CatchTreeImpl(meta(ctx), parameter, body);
    }

    @Override
    public Tree visitTryExpression(SLangParser.TryExpressionContext ctx) {
      Tree tryBlock = visit(ctx.block());
      List<CatchTree> catchTreeList = new ArrayList<>();
      for (SLangParser.CatchBlockContext catchBlockContext : ctx.catchBlock()) {
        catchTreeList.add((CatchTree) visit(catchBlockContext));
      }
      Tree finallyBlock = ctx.finallyBlock() == null ? null : visit(ctx.finallyBlock());
      return new ExceptionHandlingTreeImpl(meta(ctx), tryBlock, catchTreeList, finallyBlock);
    }

    @Override
    public Tree visitNativeBlock(SLangParser.NativeBlockContext ctx) {
      return nativeTree(ctx, ctx.statement());
    }

    @Override
    public Tree visitAssignment(SLangParser.AssignmentContext ctx) {
      Tree leftHandSide = visit(ctx.expression());
      Tree statementOrExpression = assignmentTree(ctx.statement(), ctx.assignmentOperator());
      AssignmentExpressionTree.Operator operator = ASSIGNMENT_OPERATOR_MAP.get(ctx.assignmentOperator(0).getText());
      return new AssignmentExpressionTreeImpl(meta(ctx), operator, leftHandSide, statementOrExpression);
    }

    @Override
    public Tree visitDisjunction(SLangParser.DisjunctionContext ctx) {
      return binaryTree(Operator.CONDITIONAL_OR, ctx.conjunction());
    }

    @Override
    public Tree visitConjunction(SLangParser.ConjunctionContext ctx) {
      return binaryTree(Operator.CONDITIONAL_AND, ctx.equalityComparison());
    }

    @Override
    public Tree visitEqualityComparison(SLangParser.EqualityComparisonContext ctx) {
      return binaryTree(ctx.comparison(), ctx.equalityOperator());
    }

    @Override
    public Tree visitComparison(SLangParser.ComparisonContext ctx) {
      return binaryTree(ctx.additiveExpression(), ctx.comparisonOperator());
    }

    @Override
    public Tree visitAdditiveExpression(SLangParser.AdditiveExpressionContext ctx) {
      return binaryTree(ctx.multiplicativeExpression(), ctx.additiveOperator());
    }

    @Override
    public Tree visitMultiplicativeExpression(SLangParser.MultiplicativeExpressionContext ctx) {
      return binaryTree(ctx.unaryExpression(), ctx.multiplicativeOperator());
    }

    @Override
    public Tree visitUnaryExpression(SLangParser.UnaryExpressionContext ctx) {
      if (ctx.unaryOperator() == null) {
        return visit(ctx.atomicExpression());
      } else {
        Tree operand = visit(ctx.atomicExpression());
        return new UnaryExpressionTreeImpl(meta(ctx), UnaryExpressionTree.Operator.NEGATE, operand);
      }
    }

    @Override
    public Tree visitLiteral(SLangParser.LiteralContext ctx) {
      if (ctx.StringLiteral() != null) {
        return new StringLiteralTreeImpl(meta(ctx), ctx.getText());
      } else {
        return new LiteralTreeImpl(meta(ctx), ctx.getText());
      }
    }

    @Override
    public Tree visitIdentifier(SLangParser.IdentifierContext ctx) {
      return new IdentifierTreeImpl(meta(ctx), ctx.getText());
    }

    private static TextPointer startOf(Token token) {
      return new TextPointerImpl(token.getLine(), token.getCharPositionInLine());
    }

    private static TextPointer endOf(Token token) {
      return new TextPointerImpl(token.getLine(), token.getCharPositionInLine() + token.getText().length());
    }

    private TreeMetaData meta(ParserRuleContext ctx) {
      return meta(new TextRangeImpl(startOf(ctx.start), endOf(ctx.stop)));
    }

    private TreeMetaData meta(Tree first, Tree last) {
      return meta(new TextRangeImpl(first.metaData().textRange().start(), last.metaData().textRange().end()));
    }

    private TreeMetaData meta(TextRange textRange) {
      return metaDataProvider.metaData(textRange);
    }

    private NativeTree nativeTree(ParserRuleContext ctx, List<? extends ParseTree> rawChildren) {
      List<Tree> children = list(rawChildren);
      return new NativeTreeImpl(meta(ctx), new SNativeKind(ctx), children);
    }

    private NativeTree simpleNativeTree(ParserRuleContext ctx) {
      return new NativeTreeImpl(meta(ctx), new SNativeKind(ctx, ctx.getText()), Collections.emptyList());
    }

    private List<Tree> list(List<? extends ParseTree> rawChildren) {
      return rawChildren
        .stream()
        .map(this::visit)
        .collect(toList());
    }

    private Tree binaryTree(Operator operator, List<? extends ParseTree> operands) {
      Tree result = visit(operands.get(operands.size() - 1));
      for (int i = operands.size() - 2; i >= 0; i--) {
        Tree left = visit(operands.get(i));
        result = new BinaryExpressionTreeImpl(meta(left, result), operator, left, result);
      }
      return result;
    }

    private Tree binaryTree(List<? extends ParseTree> operands, List<? extends ParseTree> operators) {
      Tree result = visit(operands.get(operands.size() - 1));
      for (int i = operands.size() - 2; i >= 0; i--) {
        Tree left = visit(operands.get(i));
        Operator operator = BINARY_OPERATOR_MAP.get(operators.get(i).getText());
        result = new BinaryExpressionTreeImpl(meta(left, result), operator, left, result);
      }
      return result;
    }

    private Tree assignmentTree(List<? extends ParseTree> expressions, List<? extends ParseTree> operators) {
      Tree result = visit(expressions.get(expressions.size() - 1));
      for (int i = expressions.size() - 2; i >= 0; i--) {
        Tree left = visit(expressions.get(i));
        AssignmentExpressionTree.Operator operator = ASSIGNMENT_OPERATOR_MAP.get(operators.get(i).getText());
        result = new AssignmentExpressionTreeImpl(meta(left, result), operator, left, result);
      }
      return result;
    }
  }
}
