/*
 * SonarSource SLang
 * Copyright (C) 2018-2022 SonarSource SA
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
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.complexity.CognitiveComplexity;
import org.sonarsource.slang.visitors.TreeVisitor;

public class MetricVisitor extends TreeVisitor<InputFileContext> {

  private static final boolean[] IS_NON_BLANK_CHAR_IN_COMMENTS = new boolean[127];
  static {
    for (int c = 0; c < IS_NON_BLANK_CHAR_IN_COMMENTS.length; c++) {
      IS_NON_BLANK_CHAR_IN_COMMENTS[c] = c > ' ' && "*#-=|".indexOf(c) == -1;
    }
  }

  public static final String NOSONAR_PREFIX = "NOSONAR";
  private final FileLinesContextFactory fileLinesContextFactory;
  private final NoSonarFilter noSonarFilter;
  private final Predicate<Tree> executableLineOfCodePredicate;

  private Set<Integer> linesOfCode;
  private Set<Integer> commentLines;
  private Set<Integer> nosonarLines;
  private Set<Integer> executableLines;
  private int numberOfFunctions;
  private int numberOfClasses;
  private int complexity;
  private int statements;
  private int cognitiveComplexity;

  public MetricVisitor(FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, Predicate<Tree> executableLineOfCodePredicate) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;
    this.executableLineOfCodePredicate = executableLineOfCodePredicate;

    register(TopLevelTree.class, (ctx, tree) -> {
      List<Tree> declarations = tree.declarations();
      int firstTokenLine = declarations.isEmpty() ? tree.textRange().end().line() : declarations.get(0).textRange().start().line();
      tree.allComments().forEach(
        comment -> addCommentMetrics(comment, commentLines, nosonarLines, firstTokenLine));
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

  private void addExecutableLines(List<Tree> trees) {
    trees.stream()
      .filter(executableLineOfCodePredicate)
      .forEach(t -> executableLines.add(t.metaData().textRange().start().line()));
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    linesOfCode = new HashSet<>();
    commentLines = new HashSet<>();
    nosonarLines = new HashSet<>();
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
    noSonarFilter.noSonarInFile(ctx.inputFile, nosonarLines());
  }

  private static void saveMetric(InputFileContext ctx, Metric<Integer> metric, Integer value) {
    ctx.sensorContext.<Integer>newMeasure()
      .on(ctx.inputFile)
      .forMetric(metric)
      .withValue(value)
      .save();
  }

  private static void addCommentMetrics(Comment comment, Set<Integer> commentLines, Set<Integer> nosonarLines, int firstTokenLine) {
    boolean isFileHeader = comment.textRange().end().line() < firstTokenLine;
    if (!isFileHeader) {
      addCommentNonEmptyLines(comment.contentRange(), comment.contentText(), isNosonarComment(comment) ? nosonarLines : commentLines);
    }
  }

  private static void addCommentNonEmptyLines(TextRange range, String content, Set<Integer> lineNumbers) {
    int startLine = range.start().line();
    if (startLine == range.end().line()) {
      if (isNotBlank(content)) {
        lineNumbers.add(startLine);
      }
    } else {
      String[] lines = content.split("\r\n|\n|\r", -1);
      for (int i = 0; i < lines.length; i++) {
        if (isNotBlank(lines[i])) {
          lineNumbers.add(startLine + i);
        }
      }
    }
  }

  private static boolean isNotBlank(String line) {
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (ch >= IS_NON_BLANK_CHAR_IN_COMMENTS.length || IS_NON_BLANK_CHAR_IN_COMMENTS[ch]) {
        return true;
      }
    }
    return false;
  }

  public static boolean isNosonarComment(Comment comment) {
    return comment.contentText().trim().toUpperCase(Locale.ENGLISH).startsWith(NOSONAR_PREFIX);
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
