/*
 * SonarSource SLang
 * Copyright (C) 2018-2023 SonarSource SA
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
package org.sonarsource.slang.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.issue.NoSonarFilter;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.visitors.TreeVisitor;

public class SkipNoSonarLinesVisitor extends TreeVisitor<InputFileContext> {

  private final NoSonarFilter noSonarFilter;

  private Set<Integer> noSonarLines;
  public SkipNoSonarLinesVisitor(NoSonarFilter noSonarFilter) {
    this.noSonarFilter = noSonarFilter;

    register(TopLevelTree.class, (ctx, tree) -> {
      List<Tree> declarations = tree.declarations();
      int firstTokenLine = declarations.isEmpty() ? tree.textRange().end().line() : declarations.get(0).textRange().start().line();
      tree.allComments()
        .forEach(comment -> noSonarLines.addAll(findNoSonarCommentLines(comment, firstTokenLine)));
    });
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    noSonarLines = new HashSet<>();
  }

  @Override
  protected void after(InputFileContext ctx, Tree root) {
    noSonarFilter.noSonarInFile(ctx.inputFile, noSonarLines);
  }

  private static Set<Integer> findNoSonarCommentLines(Comment comment, int firstTokenLine) {
    boolean isFileHeader = comment.textRange().end().line() < firstTokenLine;

    if (!isFileHeader && CommentAnalysisUtils.isNosonarComment(comment)) {
      return CommentAnalysisUtils.findNonEmptyCommentLines(comment.contentRange(), comment.contentText());
    }

    return Set.of();
  }
}
