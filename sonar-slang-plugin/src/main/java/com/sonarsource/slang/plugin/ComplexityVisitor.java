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
package com.sonarsource.slang.plugin;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.ClassDeclarationTree;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.HasTextRange;
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.LoopTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.kotlin.InputFileContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.util.ArrayList;
import java.util.List;

public class ComplexityVisitor extends TreeVisitor<InputFileContext> {

  private List<HasTextRange> complexityTrees = new ArrayList<>();
  public ComplexityVisitor() {

    register(FunctionDeclarationTree.class, (ctx, tree) -> {
      if (tree.name() != null && tree.body() != null) {
        complexityTrees.add(tree);
      }
    });

    register(ClassDeclarationTree.class, (ctx, tree) -> complexityTrees.add(tree));

    register(IfTree.class, (ctx, tree) -> {
      complexityTrees.add(tree.ifKeyword());
      if (tree.elseBranch() != null) {
        complexityTrees.add(tree.elseKeyword());
      }
    });

    register(LoopTree.class, (ctx, tree) -> complexityTrees.add(tree));

    register(MatchCaseTree.class, (ctx, tree) -> complexityTrees.add(tree));

    register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (tree.operator() == BinaryExpressionTree.Operator.CONDITIONAL_AND ||
        tree.operator() == BinaryExpressionTree.Operator.CONDITIONAL_OR) {
        complexityTrees.add(tree);
      }
    });
  }

  public List<HasTextRange> complexityTrees(InputFileContext ctx, Tree tree) {
    this.complexityTrees = new ArrayList<>();
    this.scan(ctx, tree);
    return this.complexityTrees;
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    complexityTrees = new ArrayList<>();
  }
}
