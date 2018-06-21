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

import com.sonarsource.slang.api.ClassDeclarationTree;
import com.sonarsource.slang.api.Comment;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.kotlin.InputFileContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;

public class MetricVisitor extends TreeVisitor<InputFileContext> {

  public static final String NOSONAR_PREFIX = "NOSONAR";
  private final FileLinesContextFactory fileLinesContextFactory;
  private final NoSonarFilter noSonarFilter;

  private Set<Integer> linesOfCode;
  private Set<Integer> commentLines;
  private Set<Integer> nosonarLines;
  private int numberOfFunctions;
  private int numberOfClasses;

  public MetricVisitor(FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;

    register(TopLevelTree.class, (ctx, tree) -> {
      tree.allComments().forEach(
        comment -> addCommentMetrics(comment, commentLines, nosonarLines));
      linesOfCode.addAll(tree.metaData().linesOfCode());
    });
    register(FunctionDeclarationTree.class, (ctx, tree) -> {
      if (tree.name() != null && tree.body() != null) {
        numberOfFunctions++;
      }
    });
    register(ClassDeclarationTree.class, (ctx, tree) -> {
      numberOfClasses++;
    });
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    linesOfCode = new HashSet<>();
    commentLines = new HashSet<>();
    nosonarLines = new HashSet<>();
    numberOfFunctions = 0;
    numberOfClasses = 0;
  }

  @Override
  protected void after(InputFileContext ctx, Tree root) {
    saveMetric(ctx, CoreMetrics.NCLOC, linesOfCode().size());
    saveMetric(ctx, CoreMetrics.COMMENT_LINES, commentLines().size());
    saveMetric(ctx, CoreMetrics.FUNCTIONS, numberOfFunctions());
    saveMetric(ctx, CoreMetrics.CLASSES, numberOfClasses());

    FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(ctx.inputFile);
    linesOfCode().forEach(line -> fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1));
    commentLines().forEach(line -> fileLinesContext.setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, line, 1));

    fileLinesContext.save();
    noSonarFilter.noSonarInFile(ctx.inputFile, nosonarLines());
  }

  private static void saveMetric(InputFileContext ctx, Metric<Integer> metric, Integer value) {
    ctx.sensorContext.<Integer>newMeasure()
      .on(ctx.inputFile)
      .forMetric(metric)
      .withValue(value)
      .save();
  }

  private static void addCommentMetrics(Comment comment, Set<Integer> commentLines, Set<Integer> nosonarLines) {
    add(comment.textRange(), commentLines);
    if (isNosonarComment(comment)) {
      add(comment.textRange(), nosonarLines);
    }
  }

  private static void add(TextRange range, Set<Integer> lineNumbers) {
    for (int i = range.start().line(); i <= range.end().line(); i++) {
      lineNumbers.add(i);
    }
  }

  public static boolean isNosonarComment(Comment comment) {
    return comment.text().trim().toUpperCase(Locale.ENGLISH).startsWith(NOSONAR_PREFIX);
  }

  public Set<Integer> linesOfCode() {
    return linesOfCode;
  }

  public Set<Integer> commentLines() {
    return commentLines;
  }

  public Set<Integer> nosonarLines() {
    return nosonarLines;
  }

  public int numberOfFunctions() {
    return numberOfFunctions;
  }

  public int numberOfClasses() {
    return numberOfClasses;
  }

}
