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
package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.api.ASTConverter;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.CommonCheckList;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.plugin.MetricVisitor;
import com.sonarsource.slang.plugin.SyntaxHighlighter;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class KotlinSensor implements Sensor {

  private static final Logger LOG = Loggers.get(KotlinSensor.class);

  private final Checks<SlangCheck> checks;
  private final NoSonarFilter noSonarFilter;
  private FileLinesContextFactory fileLinesContextFactory;

  public KotlinSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter) {
    checks = checkFactory.create(SlangPlugin.KOTLIN_REPOSITORY_KEY);
    checks.addAnnotatedChecks((Iterable<?>) CommonCheckList.checks());
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage("kotlin")
      .name("Kotlin Sensor");
  }

  @Override
  public void execute(SensorContext sensorContext) {
    FileSystem fileSystem = sensorContext.fileSystem();
    FilePredicate mainFilePredicate = fileSystem.predicates().and(
      fileSystem.predicates().hasLanguage(SlangPlugin.KOTLIN_LANGUAGE_KEY),
      fileSystem.predicates().hasType(InputFile.Type.MAIN));
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(mainFilePredicate);
    analyseFiles(sensorContext, inputFiles, Arrays.asList(
      new ChecksVisitor(checks),
      new MetricVisitor(fileLinesContextFactory, noSonarFilter),
      new SyntaxHighlighter()));
  }

  private static void analyseFiles(SensorContext sensorContext, Iterable<InputFile> inputFiles, List<TreeVisitor<InputFileContext>> visitors) {
    ASTConverter converter = new KotlinConverter();
    for (InputFile inputFile : inputFiles) {
      InputFileContext inputFileContext = new InputFileContext(sensorContext, inputFile);
      try {
        analyseFile(converter, inputFileContext, inputFile, visitors);
      } catch (ParseException e) {
        logParsingError(inputFile, e);
        inputFileContext.reportError("Unable to parse file: " + inputFile, e.getPosition());
      }
    }
  }

  private static void analyseFile(ASTConverter converter, InputFileContext inputFileContext, InputFile inputFile, List<TreeVisitor<InputFileContext>> visitors) {
    String content;
    try {
      content = inputFile.contents();
    } catch (IOException e) {
      throw new ParseException("Cannot read " + inputFile);
    }

    Tree tree = converter.parse(content);
    for (TreeVisitor<InputFileContext> visitor : visitors) {
      visitor.scan(inputFileContext, tree);
    }
  }

  private static void logParsingError(InputFile inputFile, ParseException e) {
    TextPointer position = e.getPosition();
    String positionMessage = "";
    if (position != null) {
      positionMessage = String.format("Parse error at position %s:%s", position.line(), position.lineOffset());
    }
    LOG.error(String.format("Unable to parse file: %s. %s", inputFile.uri(), positionMessage));
    LOG.error(e.getMessage());
  }

}
