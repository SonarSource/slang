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
package org.sonarsource.ruby.plugin;

import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonarsource.ruby.converter.RubyConverter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.plugin.SlangSensor;

public class RubySensor extends SlangSensor {

  private final Checks<SlangCheck> checks;

  public RubySensor(SonarRuntime sonarRuntime, CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, RubyLanguage language) {
    super(sonarRuntime, noSonarFilter, fileLinesContextFactory, language);
    checks = checkFactory.create(RubyPlugin.RUBY_REPOSITORY_KEY);
    checks.addAnnotatedChecks((Iterable<?>) RubyCheckList.checks());
  }

  @Override
  protected ASTConverter astConverter(SensorContext sensorContext) {
    return new RubyConverter();
  }

  @Override
  protected Checks<SlangCheck> checks() {
    return checks;
  }

  @Override
  protected String repositoryKey() {
    return RubyPlugin.RUBY_REPOSITORY_KEY;
  }

}
