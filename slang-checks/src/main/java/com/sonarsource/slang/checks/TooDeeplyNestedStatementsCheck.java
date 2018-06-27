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
package com.sonarsource.slang.checks;

import com.sonarsource.slang.api.ExceptionHandlingTree;
import com.sonarsource.slang.api.IfTree;
import com.sonarsource.slang.api.LoopTree;
import com.sonarsource.slang.api.MatchTree;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SecondaryLocation;
import com.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "S134")
public class TooDeeplyNestedStatementsCheck implements SlangCheck {
  private static final int DEFAULT_MAX_DEPTH = 3;

  @RuleProperty(
      key = "max",
      description = "Maximum allowed control flow statement nesting depth",
      defaultValue = "" + DEFAULT_MAX_DEPTH)

  public int max;

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, this::checkNestedDepth);
    init.register(LoopTree.class, this::checkNestedDepth);
    init.register(MatchTree.class, this::checkNestedDepth);
    init.register(ExceptionHandlingTree.class, this::checkNestedDepth);
  }

  private void checkNestedDepth(CheckContext ctx, Tree tree) {
    if (isElseIfStatement(ctx.parent(), tree)) {
      return;
    }

    Iterator<Tree> iterator = ctx.ancestors().iterator();
    Deque<Token> nestedParentNodes = new LinkedList<>();
    Tree last = tree;

    while (iterator.hasNext()) {
      Tree parent = iterator.next();
      if (isElseIfStatement(parent, last) && !nestedParentNodes.isEmpty()) {
        nestedParentNodes.removeLast();
      }

      if (parent instanceof LoopTree || parent instanceof ExceptionHandlingTree || parent instanceof IfTree || parent instanceof MatchTree) {
        nestedParentNodes.addLast(getNodeToHighlight(parent));
      }

      if (nestedParentNodes.size() > max) {
        return;
      }
      last = parent;
    }

    if (nestedParentNodes.size() == max) {
      reportIssue(ctx, tree, nestedParentNodes);
    }
  }

  private static boolean isElseIfStatement(@Nullable Tree parent, Tree tree) {
    return tree instanceof IfTree && parent instanceof IfTree && tree.equals(((IfTree) parent).elseBranch());
  }
  
  private void reportIssue(CheckContext ctx, Tree statement, Deque<Token> nestedStatements) {
    String message = String.format("Refactor this code to not nest more than %s control flow statements.", max);
    List<SecondaryLocation> secondaryLocations = new ArrayList<>(nestedStatements.size());
    int nestedDepth = 0;

    while (!nestedStatements.isEmpty()) {
      nestedDepth++;
      String secondaryLocationMessage = String.format("Nesting depth %s", nestedDepth);
      secondaryLocations.add(new SecondaryLocation(nestedStatements.removeLast().textRange(), secondaryLocationMessage));
    }

    Token nodeToHighlight = getNodeToHighlight(statement);
    ctx.reportIssue(nodeToHighlight, message, secondaryLocations);
  }

  private static Token getNodeToHighlight(Tree tree) {
    if (tree instanceof IfTree) {
      return ((IfTree) tree).ifKeyword();
    } else if (tree instanceof MatchTree) {
      return ((MatchTree) tree).keyword();
    } else if (tree instanceof ExceptionHandlingTree) {
      return ((ExceptionHandlingTree) tree).tryKeyword();
    } else if (tree instanceof LoopTree) {
      return ((LoopTree) tree).keyword();
    } else {
      throw new IllegalStateException("Unable to find node to highlight");
    }
  }

}
