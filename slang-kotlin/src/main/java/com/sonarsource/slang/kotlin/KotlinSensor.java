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

import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.CommonCheckList;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

public class KotlinSensor implements Sensor {

  private final Checks<SlangCheck> checks;

  public KotlinSensor(CheckFactory checkFactory) {
    checks = checkFactory.create(KotlinPlugin.REPOSITORY_KEY);
    checks.addAnnotatedChecks((Iterable<?>) CommonCheckList.checks());
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
      fileSystem.predicates().hasLanguage(KotlinPlugin.LANGUAGE_KEY),
      fileSystem.predicates().hasType(InputFile.Type.MAIN));
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(mainFilePredicate);
    analyseFiles(sensorContext, inputFiles, Collections.singletonList(new ChecksVisitor(checks)));
  }

  private void analyseFiles(SensorContext sensorContext, Iterable<InputFile> inputFiles, List<TreeVisitor<InputFileContext>> visitors) {
    for (InputFile inputFile : inputFiles) {
      analyseFile(sensorContext, inputFile, visitors);
    }
  }

  private void analyseFile(SensorContext sensorContext, InputFile inputFile, List<TreeVisitor<InputFileContext>> visitors) {
    String content;
    try {
      content = inputFile.contents();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read " + inputFile);
    }
    Tree tree = KotlinParser.fromString(content);
    InputFileContext visitorContext = new InputFileContext(sensorContext, inputFile);
    for (TreeVisitor<InputFileContext> visitor : visitors) {
      visitor.scan(visitorContext, tree);
    }
  }

}
