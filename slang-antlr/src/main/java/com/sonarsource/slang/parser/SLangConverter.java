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
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.BlockTreeImpl;
import com.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.IfTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import com.sonarsource.slang.impl.TextPointerImpl;
import com.sonarsource.slang.impl.TextRangeImpl;
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

import static java.util.stream.Collectors.toList;

public class SLangConverter {

  public Tree parse(String slangCode) {
    SLangLexer lexer = new SLangLexer(CharStreams.fromString(slangCode));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SLangParser parser = new SLangParser(tokens);

    SLangParseTreeVisitor slangVisitor = new SLangParseTreeVisitor();
    return slangVisitor.visit(parser.slangFile());
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

    @Override
    public Tree visitSlangFile(SLangParser.SlangFileContext ctx) {
      return nativeTree(ctx, ctx.typeDeclaration());
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

      return new FunctionDeclarationTreeImpl(FAKE_RANGE, modifiers, returnType, name, convertedParameters, (BlockTree) visit(ctx.methodBody()));
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
      return new BlockTreeImpl(textRange(ctx.LCURLY().getSymbol(), ctx.RCURLY().getSymbol()), list(ctx.statementOrExpression()));
    }

    @Override
    public Tree visitConditional(SLangParser.ConditionalContext ctx) {
      return visit(ctx.children.get(0));
    }

    @Override
    public Tree visitIfExpression(SLangParser.IfExpressionContext ctx) {
      Tree elseBranch = null;
      if (ctx.controlBlock().size() > 1) {
        elseBranch = visit(ctx.controlBlock(1));
      }
      Tree thenBranch = visit(ctx.controlBlock(0));
      Tree lastTree = elseBranch == null ? thenBranch : elseBranch;
      return new IfTreeImpl(textRange(startOf(ctx.start), lastTree.textRange().end()), visit(ctx.statementOrExpression()), thenBranch, elseBranch);
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
      return new LiteralTreeImpl(textRange(ctx.getStart()), ctx.getText());
    }

    @Override
    public Tree visitIdentifier(SLangParser.IdentifierContext ctx) {
      return new IdentifierTreeImpl(textRange(ctx.getStart()), ctx.getText());
    }

    private TextPointer startOf(Token token) {
      return new TextPointerImpl(token.getLine(), token.getCharPositionInLine());
    }

    private TextRange textRange(Token firstToken, Token lastToken) {
      return new TextRangeImpl(
        startOf(firstToken),
        new TextPointerImpl(lastToken.getLine(), lastToken.getCharPositionInLine() + lastToken.getText().length()));
    }

    private TextRange textRange(Token token) {
      return textRange(token, token);
    }

    private TextRange textRange(Tree first, Tree last) {
      return new TextRangeImpl(first.textRange().start(), last.textRange().end());
    }

    private TextRange textRange(TextPointer first, TextPointer last) {
      return new TextRangeImpl(first, last);
    }

    private NativeTree nativeTree(ParserRuleContext ctx, List<? extends ParseTree> rawChildren) {
      List<Tree> children = list(rawChildren);
      return new NativeTreeImpl(textRange(children.get(0), children.get(children.size() - 1)), new SNativeKind(ctx), children);
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
        result = new BinaryExpressionTreeImpl(textRange(left, result), operator, left, result);
      }
      return result;
    }

    private Tree binaryTree(List<? extends ParseTree> operands, List<? extends ParseTree> operators) {
      Tree result = visit(operands.get(operands.size() - 1));
      for (int i = operands.size() - 2; i >= 0; i--) {
        Tree left = visit(operands.get(i));
        Operator operator = BINARY_OPERATOR_MAP.get(operators.get(i).getText());
        result = new BinaryExpressionTreeImpl(textRange(left, result), operator, left, result);
      }
      return result;
    }
  }
}
