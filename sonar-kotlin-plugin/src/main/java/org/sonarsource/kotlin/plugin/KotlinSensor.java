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
package org.sonarsource.kotlin.plugin;

import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonarsource.kotlin.converter.KotlinCodeVerifier;
import org.sonarsource.kotlin.converter.KotlinConverter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.CommentedCodeCheck;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.plugin.SlangSensor;

public class KotlinSensor extends SlangSensor {

  private final Checks<SlangCheck> checks;

  public KotlinSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, KotlinLanguage language) {
    super(noSonarFilter, fileLinesContextFactory, language);

    checks = checkFactory.create(KotlinPlugin.KOTLIN_REPOSITORY_KEY);
    checks.addAnnotatedChecks((Iterable<?>) CheckList.kotlinChecks());
    checks.addAnnotatedChecks(new CommentedCodeCheck(new KotlinCodeVerifier()));
  }

  @Override
  protected ASTConverter astConverter() {
    return new KotlinConverter();
  }

  @Override
  protected Checks<SlangCheck> checks() {
    return checks;
  }

}
