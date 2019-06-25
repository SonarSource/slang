/*
 * SonarQube Go Plugin
 * Copyright (C) 2018-2019 SonarSource SA
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
package org.sonar.go.plugin;

import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.go.converter.GoConverter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.plugin.SlangSensor;

public class GoSensor extends SlangSensor {

  private final Checks<SlangCheck> checks;

  public GoSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, GoLanguage language) {
    super(noSonarFilter, fileLinesContextFactory, language);
    checks = checkFactory.create(GoRulesDefinition.REPOSITORY_KEY);
    checks.addAnnotatedChecks((Iterable<?>) GoCheckList.checks());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(GoLanguage.KEY)
      .name("SonarGo");
  }

  @Override
  protected ASTConverter astConverter(SensorContext sensorContext) {
    return new GoConverter(sensorContext.fileSystem().workDir());
  }

  @Override
  protected Checks<SlangCheck> checks() {
    return checks;
  }

  @Override
  protected String repositoryKey() {
    return GoRulesDefinition.REPOSITORY_KEY;
  }
}
