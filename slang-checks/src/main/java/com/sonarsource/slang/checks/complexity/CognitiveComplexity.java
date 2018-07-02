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
package com.sonarsource.slang.checks.complexity;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.CatchTree;
import com.sonarsource.slang.api.ClassDeclarationTree;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.LoopTree;
import com.sonarsource.slang.api.MatchTree;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.sonarsource.slang.api.BinaryExpressionTree.Operator.CONDITIONAL_AND;
import static com.sonarsource.slang.api.BinaryExpressionTree.Operator.CONDITIONAL_OR;

public class CognitiveComplexity {

  private List<Increment> increments = new ArrayList<>();

  public CognitiveComplexity (Tree root) {
    CognitiveComplexityVisitor visitor = new CognitiveComplexityVisitor();
    visitor.scan(new TreeContext(), root);
  }

  public int value() {
    int total = 0;
    for (Increment increment : increments) {
      total += increment.nestingLevel + 1;
    }
    return total;
  }

  public List<Increment> increments() {
    return increments;
  }

  public static class Increment {

    private final Token token;
    private final int nestingLevel;

    private Increment(Token token, int nestingLevel) {
      this.token = token;
      this.nestingLevel = nestingLevel;
    }

    public Token token() {
      return token;
    }

    public int nestingLevel() {
      return nestingLevel;
    }
  }

  private class CognitiveComplexityVisitor extends TreeVisitor<TreeContext> {

    private Set<Token> alreadyConsideredOperators = new HashSet<>();

    private CognitiveComplexityVisitor() {

      // TODO ternary operator
      // TODO "break" or "continue" with label

      register(LoopTree.class, (ctx, tree) -> incrementWithNesting(tree.keyword(), ctx));
      register(MatchTree.class, (ctx, tree) -> incrementWithNesting(tree.keyword(), ctx));
      register(CatchTree.class, (ctx, tree) -> incrementWithNesting(tree.keyword(), ctx));

      register(IfTree.class, (ctx, tree) -> {
        Tree parent = ctx.ancestors().peek();
        if (!(parent instanceof IfTree) || tree != ((IfTree) parent).elseBranch()) {
          incrementWithNesting(tree.ifKeyword(), ctx);
        }
        Token elseKeyword = tree.elseKeyword();
        if (elseKeyword != null) {
          incrementWithoutNesting(elseKeyword);
        }
      });

      register(BinaryExpressionTree.class, (ctx, tree) -> {
        if (!isLogicalBinaryExpression(tree) || alreadyConsideredOperators.contains(tree.operatorToken())) {
          return;
        }

        List<Token> operators = new ArrayList<>();
        flattenOperators(tree, operators);

        Token previous = null;
        for (Token operator : operators) {
          if (previous == null || !previous.text().equals(operator.text())) {
            incrementWithoutNesting(operator);
          }
          previous = operator;
          alreadyConsideredOperators.add(operator);
        }
      });
    }

    private boolean isLogicalBinaryExpression(Tree tree) {
      if (tree instanceof BinaryExpressionTree) {
        BinaryExpressionTree.Operator operator = ((BinaryExpressionTree) tree).operator();
        return operator == CONDITIONAL_AND || operator == CONDITIONAL_OR;
      }
      return false;
    }

    // TODO parentheses should probably be skipped
    private void flattenOperators(BinaryExpressionTree tree, List<Token> operators) {
      if (isLogicalBinaryExpression(tree.leftOperand())) {
        flattenOperators((BinaryExpressionTree) tree.leftOperand(), operators);
      }

      operators.add(tree.operatorToken());

      if (isLogicalBinaryExpression(tree.rightOperand())) {
        flattenOperators((BinaryExpressionTree) tree.rightOperand(), operators);
      }
    }

    private void incrementWithNesting(Token token, TreeContext ctx) {
      increment(token, nestingLevel(ctx));
    }

    private void incrementWithoutNesting(Token token) {
      increment(token, 0);
    }

    private void increment(Token token, int nestingLevel) {
      increments.add(new Increment(token, nestingLevel));
    }

    private int nestingLevel(TreeContext ctx) {
      int nestingLevel = 0;
      boolean isInsideFunction = false;
      Iterator<Tree> ancestors = ctx.ancestors().descendingIterator();
      while (ancestors.hasNext()) {
        Tree t = ancestors.next();
        if (t instanceof FunctionDeclarationTree) {
          if (isInsideFunction || nestingLevel > 0) {
            nestingLevel++;
          }
          isInsideFunction = true;
        } else if (t instanceof IfTree || t instanceof MatchTree || t instanceof LoopTree || t instanceof CatchTree) {
          nestingLevel++;
        } else if (t instanceof ClassDeclarationTree) {
          nestingLevel = 0;
          isInsideFunction = false;
        }
      }
      return nestingLevel;
    }

  }

}
