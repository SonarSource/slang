/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.complexity.CognitiveComplexity;
import org.sonarsource.slang.visitors.TreeVisitor;

public class MetricVisitor extends TreeVisitor<InputFileContext> {
  private final FileLinesContextFactory fileLinesContextFactory;
  private final Predicate<Tree> executableLineOfCodePredicate;

  private Set<Integer> linesOfCode;
  private Set<Integer> commentLines;
  private Set<Integer> executableLines;
  private int numberOfFunctions;
  private int numberOfClasses;
  private int complexity;
  private int statements;
  private int cognitiveComplexity;

  public MetricVisitor(FileLinesContextFactory fileLinesContextFactory, Predicate<Tree> executableLineOfCodePredicate) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.executableLineOfCodePredicate = executableLineOfCodePredicate;

    register(TopLevelTree.class, (ctx, tree) -> {
      List<Tree> declarations = tree.declarations();
      int firstTokenLine = declarations.isEmpty() ? tree.textRange().end().line() : declarations.get(0).textRange().start().line();
      tree.allComments()
        .forEach(comment -> commentLines.addAll(findNonEmptyCommentLines(comment, firstTokenLine)));
      addExecutableLines(declarations);
      linesOfCode.addAll(tree.metaData().linesOfCode());
      complexity = new CyclomaticComplexityVisitor().complexityTrees(tree).size();
      statements = new StatementsVisitor().statements(tree);
      cognitiveComplexity = new CognitiveComplexity(tree).value();
    });

    register(FunctionDeclarationTree.class, (ctx, tree) -> {
      if (tree.name() != null && tree.body() != null) {
        numberOfFunctions++;
      }
    });

    register(ClassDeclarationTree.class, (ctx, tree) -> numberOfClasses++);

    register(BlockTree.class, (ctx, tree) -> addExecutableLines(tree.statementOrExpressions()));
  }

  static Set<Integer> findNonEmptyCommentLines(Comment comment, int firstTokenLine) {
    boolean isFileHeader = comment.textRange().end().line() < firstTokenLine;

    if (!isFileHeader && ! CommentAnalysisUtils.isNosonarComment(comment)) {
      return CommentAnalysisUtils.findNonEmptyCommentLines(comment.contentRange(), comment.contentText());
    }

    return Set.of();
  }

  private void addExecutableLines(List<Tree> trees) {
    trees.stream()
      .filter(executableLineOfCodePredicate)
      .forEach(t -> executableLines.add(t.metaData().textRange().start().line()));
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    linesOfCode = new HashSet<>();
    commentLines = new HashSet<>();
    executableLines = new HashSet<>();
    numberOfFunctions = 0;
    numberOfClasses = 0;
    complexity = 0;
    cognitiveComplexity = 0;
  }

  @Override
  protected void after(InputFileContext ctx, Tree root) {
    saveMetric(ctx, CoreMetrics.NCLOC, linesOfCode().size());
    saveMetric(ctx, CoreMetrics.COMMENT_LINES, commentLines().size());
    saveMetric(ctx, CoreMetrics.FUNCTIONS, numberOfFunctions());
    saveMetric(ctx, CoreMetrics.CLASSES, numberOfClasses());
    saveMetric(ctx, CoreMetrics.COMPLEXITY, complexity);
    saveMetric(ctx, CoreMetrics.STATEMENTS, statements);
    saveMetric(ctx, CoreMetrics.COGNITIVE_COMPLEXITY, cognitiveComplexity);

    FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(ctx.inputFile);
    linesOfCode().forEach(line -> fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1));
    executableLines().forEach(line -> fileLinesContext.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1));
    fileLinesContext.save();
  }

  private static void saveMetric(InputFileContext ctx, Metric<Integer> metric, Integer value) {
    ctx.sensorContext.<Integer>newMeasure()
      .on(ctx.inputFile)
      .forMetric(metric)
      .withValue(value)
      .save();
  }


  public Set<Integer> linesOfCode() {
    return linesOfCode;
  }

  public Set<Integer> commentLines() {
    return commentLines;
  }

  public Set<Integer> executableLines() {
    return executableLines;
  }

  public int numberOfFunctions() {
    return numberOfFunctions;
  }

  public int numberOfClasses() {
    return numberOfClasses;
  }

  public int cognitiveComplexity() {
    return cognitiveComplexity;
  }

}
