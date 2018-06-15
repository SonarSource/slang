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
import com.sonarsource.slang.api.Comment;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.ParameterTree;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import com.sonarsource.slang.api.UnaryExpressionTree;
import com.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.BlockTreeImpl;
import com.sonarsource.slang.impl.CommentImpl;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.sonarsource.analyzer.commons.TokenLocation;

import static java.util.stream.Collectors.toList;

public class SLangConverter implements ASTConverter {

  @Override
  public Tree parse(String slangCode) {
    SLangLexer lexer = new SLangLexer(CharStreams.fromString(slangCode));

    List<Comment> comments = new ArrayList<>();
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    tokens.fill();

    for (int index = 0; index < tokens.size(); index++) {
      Token token = tokens.get(index);
      if (token.getChannel() == 1) {
        TokenLocation location = new TokenLocation(token.getLine(), token.getCharPositionInLine(), token.getText());
        TextRange textRange = new TextRangeImpl(location.startLine(), location.startLineOffset(), location.endLine(), location.endLineOffset());
        comments.add(new CommentImpl(commentContent(token.getText()), token.getText(), textRange));
      }
    }

    SLangParser parser = new SLangParser(tokens);

    SLangParseTreeVisitor slangVisitor = new SLangParseTreeVisitor(comments);
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

  private static class SLangParseTreeVisitor extends SLangBaseVisitor<Tree> {

    private final TreeMetaDataProvider metaDataProvider;

    public SLangParseTreeVisitor(List<Comment> comments) {
      metaDataProvider = new TreeMetaDataProvider(comments);
    }

    @Override
    public Tree visitSlangFile(SLangParser.SlangFileContext ctx) {
      // Special case for text range here, as last token is <EOF> which has length 5, so we only go up to the start of the <EOF> token
      TextRangeImpl textRange = new TextRangeImpl(startOf(ctx.start), new TextPointerImpl(ctx.stop.getLine(), ctx.stop.getCharPositionInLine()));
      return new TopLevelTreeImpl(meta(textRange, Collections.emptyList()), list(ctx.typeDeclaration()), metaDataProvider.allComments());
    }

    @Override
    public Tree visitNativeExpression(SLangParser.NativeExpressionContext ctx) {
      return nativeTree(ctx, ctx.nativeBlock());
    }

    @Override
    public Tree visitParenthesizedExpression(SLangParser.ParenthesizedExpressionContext ctx) {
      return nativeTree(ctx, Collections.singletonList(ctx.statementOrExpression()));
    }

    @Override
    public Tree visitStatementOrExpression(SLangParser.StatementOrExpressionContext ctx) {
      return assignmentTree(ctx, ctx.disjunction(), ctx.assignmentOperator());
    }

    @Override
    public Tree visitMethodDeclaration(SLangParser.MethodDeclarationContext ctx) {
      List<com.sonarsource.slang.api.Token> tokens = new ArrayList<>(tokens(ctx));
      List<Tree> modifiers = list(ctx.methodModifier());
      Tree returnType = null;
      IdentifierTree name = null;
      SLangParser.MethodHeaderContext methodHeaderContext = ctx.methodHeader();
      tokens.addAll(tokens(methodHeaderContext));
      SLangParser.ResultContext resultContext = methodHeaderContext.result();
      SLangParser.IdentifierContext identifier = methodHeaderContext.methodDeclarator().identifier();
      if (resultContext != null) {
        String resultText = resultContext.getText();
        TokenImpl resultToken = new TokenImpl(meta(resultContext).textRange(), resultText, false);
        returnType = new IdentifierTreeImpl(meta(resultContext, Collections.singletonList(resultToken)), resultText);
      }
      if (identifier != null) {
        name = (IdentifierTree) visit(identifier);
      }

      tokens.addAll(tokens(methodHeaderContext.methodDeclarator()));
      List<ParameterTree> convertedParameters = new ArrayList<>();
      SLangParser.FormalParameterListContext formalParameterListContext = methodHeaderContext.methodDeclarator().formalParameterList();
      if (formalParameterListContext != null) {
        tokens.addAll(tokens(formalParameterListContext));
        SLangParser.FormalParametersContext formalParameters = formalParameterListContext.formalParameters();
        if (formalParameters != null) {
          convertedParameters.addAll(list(formalParameters.formalParameter()).stream().map(ParameterTree.class::cast).collect(toList()));
        }
        convertedParameters.add((ParameterTree) visit(formalParameterListContext.lastFormalParameter()));
      }

      BlockTree body = (BlockTree) visit(ctx.methodBody());
      if (body == null) {
        tokens.addAll(tokens(ctx.methodBody()));
      }
      return new FunctionDeclarationTreeImpl(meta(ctx, tokens), modifiers, returnType, name, convertedParameters, body);
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
        type = visit(ctx.simpleType());
      }
      return new ParameterTreeImpl(meta(ctx), tree, type);
    }

    @Override
    public Tree visitLastFormalParameter(SLangParser.LastFormalParameterContext ctx) {
      return visit(ctx.formalParameter());
    }

    @Override
    public Tree visitBlock(SLangParser.BlockContext ctx) {
      return new BlockTreeImpl(meta(ctx), list(ctx.statementOrExpression()));
    }

    @Override
    public Tree visitIfExpression(SLangParser.IfExpressionContext ctx) {
      Tree elseBranch = null;
      if (ctx.controlBlock().size() > 1) {
        elseBranch = visit(ctx.controlBlock(1));
      }
      Tree thenBranch = visit(ctx.controlBlock(0));
      return new IfTreeImpl(meta(ctx), visit(ctx.statementOrExpression()), thenBranch, elseBranch);
    }

    @Override
    public Tree visitMatchExpression(SLangParser.MatchExpressionContext ctx) {
      List<MatchCaseTree> cases = new ArrayList<>();
      for (SLangParser.MatchCaseContext matchCaseContext : ctx.matchCase()) {
        cases.add((MatchCaseTree) visit(matchCaseContext));
      }
      return new MatchTreeImpl(meta(ctx), visit(ctx.statementOrExpression()), cases);
    }

    @Override
    public Tree visitMatchCase(SLangParser.MatchCaseContext ctx) {
      Tree expression = ctx.statementOrExpression() == null ? null : visit(ctx.statementOrExpression());
      Tree body = visit(ctx.controlBlock());
      return new MatchCaseTreeImpl(meta(ctx), expression, body);
    }

    @Override
    public Tree visitNativeBlock(SLangParser.NativeBlockContext ctx) {
      return nativeTree(ctx, ctx.statementOrExpression());
    }

    @Override
    public Tree visitAssignment(SLangParser.AssignmentContext ctx) {
      Tree leftHandSide = visit(ctx.leftHandSide());
      Tree statementOrExpression = visit(ctx.statementOrExpression());
      AssignmentExpressionTree.Operator operator = ASSIGNMENT_OPERATOR_MAP.get(ctx.assignmentOperator().getText());
      return new AssignmentExpressionTreeImpl(meta(ctx, tokens(ctx.assignmentOperator())), operator, leftHandSide, statementOrExpression);
    }

    @Override
    public Tree visitDisjunction(SLangParser.DisjunctionContext ctx) {
      return binaryTree(ctx, Operator.CONDITIONAL_OR, ctx.conjunction());
    }

    @Override
    public Tree visitConjunction(SLangParser.ConjunctionContext ctx) {
      return binaryTree(ctx, Operator.CONDITIONAL_AND, ctx.equalityComparison());
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
        return new UnaryExpressionTreeImpl(meta(ctx, tokens(ctx.unaryOperator())), UnaryExpressionTree.Operator.NEGATE, operand);
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

    private static List<com.sonarsource.slang.api.Token> tokens(ParserRuleContext ctx) {
      boolean isInIdentifier = ctx instanceof SLangParser.IdentifierContext;
      return ctx.children.stream()
        .map(c -> (c instanceof SLangParser.SemiContext) ? ((SLangParser.SemiContext) c).SEMICOLON() : c)
        .filter(c -> c instanceof TerminalNode)
        .map(c -> ((TerminalNode) c))
        .map(c -> new TokenImpl(range(c.getSymbol()), c.getText(), !isInIdentifier && Character.isAlphabetic(c.getText().charAt(0))))
        .collect(Collectors.toList());
    }

    private static TextPointer startOf(Token token) {
      return new TextPointerImpl(token.getLine(), token.getCharPositionInLine());
    }

    private static TextPointer endOf(Token token) {
      return new TextPointerImpl(token.getLine(), token.getCharPositionInLine() + token.getText().length());
    }

    private static TextRange range(Token token) {
      return range(token, token);
    }

    private static TextRange range(Token first, Token last) {
      return new TextRangeImpl(startOf(first), endOf(last));
    }

    private static TextRange range(Tree first, Tree last) {
      return new TextRangeImpl(first.metaData().textRange().start(), last.metaData().textRange().end());
    }

    private TreeMetaData meta(ParserRuleContext ctx) {
      return meta(range(ctx.start, ctx.stop), tokens(ctx));
    }

    private TreeMetaData meta(ParserRuleContext ctx, List<com.sonarsource.slang.api.Token> tokens) {
      return meta(range(ctx.start, ctx.stop), tokens);
    }

    private TreeMetaData meta(Tree first, Tree last, List<com.sonarsource.slang.api.Token> tokens) {
      return meta(range(first, last), tokens);
    }

    private TreeMetaData meta(TextRange textRange, List<com.sonarsource.slang.api.Token> tokens) {
      return metaDataProvider.metaData(textRange, tokens);
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

    private Tree binaryTree(ParserRuleContext ctx, Operator operator, List<? extends ParseTree> operands) {
      Tree result = visit(operands.get(operands.size() - 1));
      List<com.sonarsource.slang.api.Token> tokens = tokens(ctx);
      for (int i = operands.size() - 2; i >= 0; i--) {
        Tree left = visit(operands.get(i));
        result = new BinaryExpressionTreeImpl(meta(left, result, Collections.singletonList(tokens.get(i))), operator, left, result);
      }
      return result;
    }

    private Tree binaryTree(List<? extends ParseTree> operands, List<? extends ParserRuleContext> operators) {
      Tree result = visit(operands.get(operands.size() - 1));
      for (int i = operands.size() - 2; i >= 0; i--) {
        Tree left = visit(operands.get(i));
        Operator operator = BINARY_OPERATOR_MAP.get(operators.get(i).getText());
        result = new BinaryExpressionTreeImpl(meta(left, result, tokens(operators.get(i))), operator, left, result);
      }
      return result;
    }

    private Tree assignmentTree(SLangParser.StatementOrExpressionContext ctx, List<? extends ParseTree> expressions, List<? extends ParserRuleContext> operators) {
      Tree result = visit(expressions.get(expressions.size() - 1));
      for (int i = expressions.size() - 2; i >= 0; i--) {
        Tree left = visit(expressions.get(i));
        AssignmentExpressionTree.Operator operator = ASSIGNMENT_OPERATOR_MAP.get(operators.get(i).getText());
        result = new AssignmentExpressionTreeImpl(meta(left, result, tokens(operators.get(i))), operator, left, result);
      }
      if (ctx.semi() != null) {
        return new NativeTreeImpl(meta(ctx), new SNativeKind(ctx), Collections.singletonList(result));
      }
      return result;
    }
  }
}
