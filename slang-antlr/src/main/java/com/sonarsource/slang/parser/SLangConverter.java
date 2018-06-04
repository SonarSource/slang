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

import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.Comment;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
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
import com.sonarsource.slang.impl.TextPointerImpl;
import com.sonarsource.slang.impl.TextRangeImpl;
import com.sonarsource.slang.impl.TopLevelTreeImpl;
import com.sonarsource.slang.impl.TreeMetaDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sonarsource.analyzer.commons.TokenLocation;

import static java.util.stream.Collectors.toList;

public class SLangConverter {

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

  private String commentContent(String text) {
    if (text.startsWith("//")) {
      return text.substring(2);
    }
    return text.substring(2, text.length() -2);
  }

  private static final Map<String, Operator> BINARY_OPERATOR_MAP = binaryOperatorMap();

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

  // FIXME
  private static final TextRange FAKE_RANGE = new TextRangeImpl(new TextPointerImpl(-1, -1), new TextPointerImpl(-1, -1));

  private static class SLangParseTreeVisitor extends SLangBaseVisitor<Tree> {

    private final TreeMetaDataProvider metaDataProvider;

    public SLangParseTreeVisitor(List<Comment> comments) {
      metaDataProvider = new TreeMetaDataProvider(comments);
    }

    @Override
    public Tree visitSlangFile(SLangParser.SlangFileContext ctx) {
      // Special case for text range here, as last token is <EOF> which has length 5, so we only go up to the start of the <EOF> token
      TextRangeImpl textRange = new TextRangeImpl(startOf(ctx.start), new TextPointerImpl(ctx.stop.getLine(), ctx.stop.getCharPositionInLine()));
      return new TopLevelTreeImpl(meta(textRange), list(ctx.typeDeclaration()));
    }

    @Override
    public Tree visitNativeExpression(SLangParser.NativeExpressionContext ctx) {
      return nativeTree(ctx, ctx.nativeBlock());
    }

    @Override
    public Tree visitStatementOrExpression(SLangParser.StatementOrExpressionContext ctx) {
      if (ctx.disjunction().size() == 1) {
        return visit(ctx.disjunction(0));
      }
      return nativeTree(ctx, ctx.disjunction());
    }

    @Override
    public Tree visitMethodDeclaration(SLangParser.MethodDeclarationContext ctx) {
      List<Tree> modifiers = Collections.emptyList();
      Tree returnType = null; // FIXME
      IdentifierTree name = (IdentifierTree) visit(ctx.methodHeader().methodDeclarator().identifier());

      List<Tree> convertedParameters = new ArrayList<>();
      SLangParser.FormalParameterListContext formalParameterListContext = ctx.methodHeader().methodDeclarator().formalParameterList();
      if (formalParameterListContext != null) {
        SLangParser.FormalParametersContext formalParameters = formalParameterListContext.formalParameters();
        if (formalParameters != null) {
          convertedParameters.addAll(list(formalParameters.formalParameter()));
        }
        convertedParameters.add(visit(formalParameterListContext.lastFormalParameter()));
      }

      return new FunctionDeclarationTreeImpl(meta(FAKE_RANGE), modifiers, returnType, name, convertedParameters, (BlockTree) visit(ctx.methodBody()));
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
      return visit(ctx.variableDeclaratorId().identifier());
    }

    @Override
    public Tree visitLastFormalParameter(SLangParser.LastFormalParameterContext ctx) {
      return visit(ctx.formalParameter());
    }

    @Override
    public Tree visitBlock(SLangParser.BlockContext ctx) {
      return new BlockTreeImpl(meta(ctx.start, ctx.stop), list(ctx.statementOrExpression()));
    }

    @Override
    public Tree visitIfExpression(SLangParser.IfExpressionContext ctx) {
      Tree elseBranch = null;
      if (ctx.controlBlock().size() > 1) {
        elseBranch = visit(ctx.controlBlock(1));
      }
      Tree thenBranch = visit(ctx.controlBlock(0));
      return new IfTreeImpl(meta(ctx.start, ctx.stop), visit(ctx.statementOrExpression()), thenBranch, elseBranch);
    }

    @Override
    public Tree visitMatchExpression(SLangParser.MatchExpressionContext ctx) {
      List<MatchCaseTree> cases = new ArrayList<>();
      for (SLangParser.MatchCaseContext matchCaseContext : ctx.matchCase()) {
        cases.add((MatchCaseTree) visit(matchCaseContext));
      }
      return new MatchTreeImpl(meta(ctx.start, ctx.stop), visit(ctx.statementOrExpression()), cases);
    }

    @Override
    public Tree visitMatchCase(SLangParser.MatchCaseContext ctx) {
      Tree expression = ctx.statementOrExpression() == null ? null : visit(ctx.statementOrExpression());
      Tree body = visit(ctx.controlBlock());
      return new MatchCaseTreeImpl(meta(ctx.start, ctx.stop), expression, body);
    }

    @Override
    public Tree visitNativeBlock(SLangParser.NativeBlockContext ctx) {
      return nativeTree(ctx, ctx.statementOrExpression());
    }

    @Override
    public Tree visitAssignment(SLangParser.AssignmentContext ctx) {
      return nativeTree(ctx, Arrays.asList(ctx.leftHandSide(), ctx.statementOrExpression()));
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
      return binaryTree(ctx.atomicExpression(), ctx.multiplicativeOperator());
    }

    @Override
    public Tree visitLiteral(SLangParser.LiteralContext ctx) {
      return new LiteralTreeImpl(meta(ctx.start, ctx.stop), ctx.getText());
    }

    @Override
    public Tree visitIdentifier(SLangParser.IdentifierContext ctx) {
      return new IdentifierTreeImpl(meta(ctx.start, ctx.stop), ctx.getText());
    }

    private TextPointer startOf(Token token) {
      return new TextPointerImpl(token.getLine(), token.getCharPositionInLine());
    }

    private TreeMetaData meta(Token firstToken, Token lastToken) {
      return meta(new TextRangeImpl(
        startOf(firstToken),
        new TextPointerImpl(lastToken.getLine(), lastToken.getCharPositionInLine() + lastToken.getText().length())));
    }

    private TreeMetaData meta(Tree first, Tree last) {
      return meta(new TextRangeImpl(first.metaData().textRange().start(), last.metaData().textRange().end()));
    }

    private TreeMetaData meta(TextRange textRange) {
      return metaDataProvider.metaData(textRange);
    }

    private NativeTree nativeTree(ParserRuleContext ctx, List<? extends ParseTree> rawChildren) {
      List<Tree> children = list(rawChildren);
      return new NativeTreeImpl(meta(ctx.start, ctx.stop), new SNativeKind(ctx), children);
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
  }
}
