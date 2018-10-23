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
package org.sonarsource.scala.plugin;

import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonarsource.scala.converter.ScalaCodeVerifier;
import org.sonarsource.scala.converter.ScalaConverter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.checks.CommentedCodeCheck;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.plugin.SlangSensor;

public class ScalaSensor  extends SlangSensor {

  private final Checks<SlangCheck> checks;

  public ScalaSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, ScalaLanguage language) {
    super(noSonarFilter, fileLinesContextFactory, language);
    checks = checkFactory.create(ScalaPlugin.SCALA_REPOSITORY_KEY);
    checks.addAnnotatedChecks((Iterable<?>) ScalaCheckList.checks());
    checks.addAnnotatedChecks(new CommentedCodeCheck(new ScalaCodeVerifier()));
  }

  @Override
  protected ASTConverter astConverter() {
    return new ScalaConverter();
  }

  @Override
  protected Checks<SlangCheck> checks() {
    return checks;
  }

  @Override
  protected String repositoryKey() {
    return ScalaPlugin.SCALA_REPOSITORY_KEY;
  }

}
