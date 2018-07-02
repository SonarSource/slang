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

import com.sonarsource.slang.api.ASTConverter;
import com.sonarsource.slang.api.Comment;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.impl.TextRangeImpl;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;

@Rule(key = "S125")
public class CommentedCodeCheck implements SlangCheck {
  private ASTConverter commentAnalyser;

  public CommentedCodeCheck(ASTConverter astConverter) {
    this.commentAnalyser = astConverter;
  }

  private static final String MESSAGE = "Remove this commented out code.";

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> {
      List<List<Comment>> groupedComments =
        groupComments(tree.allComments());
      groupedComments.forEach(comments -> {
        String content = comments.stream()
          .map(Comment::contentText)
          .collect(Collectors.joining(""));
        if (commentAnalyser.parse(content) != null) {
          TextPointer start = comments.get(0).textRange().start();
          TextPointer end = comments.get(comments.size() - 1).textRange().end();
          TextRange textRange = new TextRangeImpl(start.line(), start.lineOffset(), end.line(), end.lineOffset());
          ctx.reportIssue(textRange, MESSAGE);
        }
      });
    });
  }

  private static List<List<Comment>> groupComments(List<Comment> comments) {
    List<List<Comment>> groups = new ArrayList<>();
    List<Comment> currentGroup = null;
    for (Comment comment : comments) {
      if (currentGroup == null) {
        currentGroup = initNewGroup(comment);
      } else if (areAdjacent(currentGroup.get(currentGroup.size() - 1), comment)) {
        currentGroup.add(comment);
      } else {
        groups.add(currentGroup);
        currentGroup = initNewGroup(comment);
      }
    }
    if (currentGroup != null) {
      groups.add(currentGroup);
    }
    return groups;
  }

  private static List<Comment> initNewGroup(Comment comment) {
    List<Comment> group = new LinkedList<>();
    group.add(comment);
    return group;
  }

  private static boolean areAdjacent(Comment commentA, Comment commentB) {
    return commentA.textRange().start().line() + 1 == commentB.textRange().start().line();
  }

}
