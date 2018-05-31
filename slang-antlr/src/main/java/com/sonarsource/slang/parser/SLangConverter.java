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

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class SLangConverter {

  public Tree parse(String slangFile) throws IOException {
    SLangLexer lexer = new SLangLexer(CharStreams.fromFileName(slangFile));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SLangParser parser = new SLangParser(tokens);

    SLangParseTreeVisitor slangVisitor = new SLangParseTreeVisitor();
    return slangVisitor.visit(parser.slangFile());
  }


  private static class SLangParseTreeVisitor extends SLangBaseVisitor<Tree> {

    @Override
    public Tree visitSlangFile(SLangParser.SlangFileContext ctx) {
      List<Tree> children = ctx.typeDeclaration()
          .stream()
          .map(type -> visit(type))
          .collect(toList());

      return new NativeTreeImpl(null, new SNativeKind(), children);
    }

    @Override
    public Tree visitNativeExpression(SLangParser.NativeExpressionContext ctx) {
      List<Tree> children = ctx.nativeBlock()
              .stream()
              .map(nativeBlockContext -> visit(nativeBlockContext))
              .collect(toList());

      return new NativeTreeImpl(null, new SNativeKind(), children);
    }

    @Override
    public Tree visitStatementOrExpression(SLangParser.StatementOrExpressionContext ctx) {
      List<Tree> children = ctx.disjunction()
              .stream()
              .map(disjunctionContext -> visit(disjunctionContext))
              .collect(toList());

      return new NativeTreeImpl(null, new SNativeKind(), children);
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
      List<Tree> children = ctx.statementOrExpression()
              .stream()
              .map(statementOrExpressionContext -> visit(statementOrExpressionContext))
              .collect(toList());
      return new NativeTreeImpl(null, new SNativeKind(), children);
    }

    @Override
    public Tree visitNativeBlock(SLangParser.NativeBlockContext ctx) {
      List<Tree> children = ctx.statementOrExpression()
              .stream()
              .map(statementOrExpressionContext -> visit(statementOrExpressionContext))
              .collect(toList());
      return new NativeTreeImpl(null, new SNativeKind(), children);
    }

    @Override
    public Tree visitDisjunction(SLangParser.DisjunctionContext ctx) {
      int conjunctionSize = ctx.conjunction().size();

      if (conjunctionSize > 1) {
        if (conjunctionSize > 2) {
          List<Tree> children = ctx.conjunction()
                  .stream()
                  .map(conjunctionContext -> visit(conjunctionContext))
                  .collect(toList());

          return new NativeTreeImpl(null, new SNativeKind(), children);
        }

        Tree left = visit(ctx.conjunction(0));
        Tree right = visit(ctx.conjunction(1));
        return new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.CONDITIONAL_OR, left, right);
      }

      return visitChildren(ctx);
    }

    @Override
    public Tree visitConjunction(SLangParser.ConjunctionContext ctx) {
      int equalitySize = ctx.equalityComparison().size();

      if (equalitySize > 1) {
        if (equalitySize > 2) {
          List<Tree> children = ctx.equalityComparison()
                  .stream()
                  .map(equalityComparisonContext -> visit(equalityComparisonContext))
                  .collect(toList());

          return new NativeTreeImpl(null, new SNativeKind(), children);
        }

        Tree left = visit(ctx.equalityComparison(0));
        Tree right = visit(ctx.equalityComparison(1));

        return new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.CONDITIONAL_AND, left, right);
      }

      return visitChildren(ctx);
    }

    @Override
    public Tree visitEqualityComparison(SLangParser.EqualityComparisonContext ctx) {
      int comparisonSize = ctx.comparison().size();

      if (comparisonSize > 1) {
        if (comparisonSize > 2) {
          List<Tree> children = ctx.comparison()
                  .stream()
                  .map(comparisonContext -> visit(comparisonContext))
                  .collect(toList());

          return new NativeTreeImpl(null, new SNativeKind(), children);
        }

        Tree left = visit(ctx.comparison(0));
        Tree right = visit(ctx.comparison(1));

        BinaryExpressionTree.Operator op;

        switch (ctx.equalityOperator(0).getText()) {
          case "!=" : op = BinaryExpressionTree.Operator.NOT_EQUAL_TO;
                      break;
          default : op = BinaryExpressionTree.Operator.EQUAL_TO;
                      break;

        }

        return new BinaryExpressionTreeImpl(null, op, left, right);
      }

      return visitChildren(ctx);
    }

    @Override
    public Tree visitComparison(SLangParser.ComparisonContext ctx) {
      int additiveSize = ctx.additiveExpression().size();

      if (additiveSize > 1) {
        if (additiveSize > 2) {
          List<Tree> children = ctx.additiveExpression()
                  .stream()
                  .map(additiveExpressionContext -> visit(additiveExpressionContext))
                  .collect(toList());

          return new NativeTreeImpl(null, new SNativeKind(), children);
        }

        Tree left = visit(ctx.additiveExpression(0));
        Tree right = visit(ctx.additiveExpression(1));

        BinaryExpressionTree.Operator op;

        switch (ctx.comparisonOperator(0).getText()) {
          case ">" : op = BinaryExpressionTree.Operator.GREATER_THAN;
            break;
          case ">=" : op = BinaryExpressionTree.Operator.GREATER_THAN_OR_EQUAL_TO;
            break;
          case "<" : op = BinaryExpressionTree.Operator.LESS_THAN;
            break;
          default : op = BinaryExpressionTree.Operator.LESS_THAN_OR_EQUAL_TO;
            break;
        }

        return new BinaryExpressionTreeImpl(null, op, left, right);
      }

      return visitChildren(ctx);
    }

    @Override
    public Tree visitAdditiveExpression(SLangParser.AdditiveExpressionContext ctx) {
      int multiplicativeSize = ctx.multiplicativeExpression().size();

      if (multiplicativeSize > 1) {
        if (multiplicativeSize > 2) {
          List<Tree> children = ctx.multiplicativeExpression()
                  .stream()
                  .map(multiplicativeExpressionContext -> visit(multiplicativeExpressionContext))
                  .collect(toList());

          return new NativeTreeImpl(null, new SNativeKind(), children);
        }

        Tree left = visit(ctx.multiplicativeExpression(0));
        Tree right = visit(ctx.multiplicativeExpression(1));

        BinaryExpressionTree.Operator op;

        switch (ctx.additiveOperator(0).getText()) {
          case "+" : op = BinaryExpressionTree.Operator.PLUS;
            break;
          default : op = BinaryExpressionTree.Operator.MINUS;
            break;
        }

        return new BinaryExpressionTreeImpl(null, op, left, right);
      }

      return visitChildren(ctx);
    }

    @Override
    public Tree visitMultiplicativeExpression(SLangParser.MultiplicativeExpressionContext ctx) {
      int atomicExpressionSize = ctx.atomicExpression().size();

      if (atomicExpressionSize > 1) {
        if (atomicExpressionSize > 2) {
          List<Tree> children = ctx.atomicExpression()
                  .stream()
                  .map(atomicExpressionContext -> visit(atomicExpressionContext))
                  .collect(toList());

          return new NativeTreeImpl(null, new SNativeKind(), children);
        }

        Tree left = visit(ctx.atomicExpression(0));
        Tree right = visit(ctx.atomicExpression(1));

        BinaryExpressionTree.Operator op;

        switch (ctx.multiplicativeOperator(0).getText()) {
          case "*" : op = BinaryExpressionTree.Operator.TIMES;
            break;
          default : op = BinaryExpressionTree.Operator.DIVIDED_BY;
            break;
        }

        return new BinaryExpressionTreeImpl(null, op, left, right);
      }

      return visitChildren(ctx);
    }


    @Override
    public Tree visitLiteral(SLangParser.LiteralContext ctx) {
      return new LiteralTreeImpl(null, ctx.getText());
    }

    @Override
    public Tree visitIdentifier(SLangParser.IdentifierContext ctx) {
      return new IdentifierImpl(null, ctx.getText());
    }
  }
}
