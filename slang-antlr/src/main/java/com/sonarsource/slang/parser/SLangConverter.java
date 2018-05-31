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
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import static java.util.stream.Collectors.toList;

public class SLangConverter {

  public Tree parse(String slangFile) throws IOException {
    SLangLexer lexer = new SLangLexer(CharStreams.fromFileName(slangFile));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SLangParser parser = new SLangParser(tokens);

    SLangParseTreeVisitor slangVisitor = new SLangParseTreeVisitor();
    return slangVisitor.visit(parser.slangFile());
  }

  private static final Map<String,Operator> BINARY_OPERATOR_MAP = binaryOperatorMap();

  private static Map<String,Operator> binaryOperatorMap() {
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

  private static class SLangParseTreeVisitor extends SLangBaseVisitor<Tree> {

    @Override
    public Tree visitSlangFile(SLangParser.SlangFileContext ctx) {
      return nativeTree(ctx.typeDeclaration());
    }

    @Override
    public Tree visitNativeExpression(SLangParser.NativeExpressionContext ctx) {
      return nativeTree(ctx.nativeBlock());
    }

    @Override
    public Tree visitStatementOrExpression(SLangParser.StatementOrExpressionContext ctx) {
      return nativeTree(ctx.disjunction());
    }

    @Override
    public Tree visitMethodDeclaration(SLangParser.MethodDeclarationContext ctx) {
      return visit(ctx.methodBody());
    }

    @Override
    public Tree visitMethodBody(SLangParser.MethodBodyContext ctx) {
      if (ctx.SEMICOLON() != null) {
        return new NativeTreeImpl(null, new SNativeKind(), new ArrayList<>());
      }
      return visit(ctx.block());
    }

    @Override
    public Tree visitBlock(SLangParser.BlockContext ctx) {
      return nativeTree(ctx.statementOrExpression());
    }

    @Override
    public Tree visitNativeBlock(SLangParser.NativeBlockContext ctx) {
      return nativeTree(ctx.statementOrExpression());
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
      return binaryTree(ctx, ctx.comparison(), ctx.equalityOperator());
    }

    @Override
    public Tree visitComparison(SLangParser.ComparisonContext ctx) {
      return binaryTree(ctx, ctx.additiveExpression(), ctx.comparisonOperator());
    }

    @Override
    public Tree visitAdditiveExpression(SLangParser.AdditiveExpressionContext ctx) {
      return binaryTree(ctx, ctx.multiplicativeExpression(), ctx.additiveOperator());
    }

    @Override
    public Tree visitMultiplicativeExpression(SLangParser.MultiplicativeExpressionContext ctx) {
      return binaryTree(ctx, ctx.atomicExpression(), ctx.multiplicativeOperator());
    }

    @Override
    public Tree visitLiteral(SLangParser.LiteralContext ctx) {
      return new LiteralTreeImpl(null, ctx.getText());
    }

    @Override
    public Tree visitIdentifier(SLangParser.IdentifierContext ctx) {
      return new IdentifierImpl(null, ctx.getText());
    }

    private NativeTree nativeTree(List<? extends org.antlr.v4.runtime.tree.ParseTree> children) {
      List<Tree> convertedChildren = children
        .stream()
        .map(this::visit)
        .collect(toList());
      return new NativeTreeImpl(null, new SNativeKind(), convertedChildren);
    }

    private Tree binaryTree(ParserRuleContext ctx, Operator operator, List<? extends ParseTree> operands) {
      if (operands.size() > 1) {
        if (operands.size() > 2) {
          return nativeTree(operands);
        }

        Tree left = visit(operands.get(0));
        Tree right = visit(operands.get(1));
        return new BinaryExpressionTreeImpl(null, operator, left, right);
      }

      return visitChildren(ctx);
    }

    private Tree binaryTree(ParserRuleContext ctx, List<? extends ParseTree> operands, List<? extends ParseTree> operators) {
      if (operands.size() > 1) {
        if (operands.size() > 2) {
          return nativeTree(operands);
        }

        Tree left = visit(operands.get(0));
        Tree right = visit(operands.get(1));
        return new BinaryExpressionTreeImpl(null, BINARY_OPERATOR_MAP.get(operators.get(0).getText()), left, right);
      }

      return visitChildren(ctx);
    }

  }
}
