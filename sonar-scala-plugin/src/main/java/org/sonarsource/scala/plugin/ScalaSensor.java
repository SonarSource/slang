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

import java.util.Map;
import java.util.function.Predicate;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonarsource.scala.converter.ScalaConverter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.ImportDeclarationTreeImpl;
import org.sonarsource.slang.impl.IntegerLiteralTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.PackageDeclarationTreeImpl;
import org.sonarsource.slang.impl.StringLiteralTreeImpl;
import org.sonarsource.slang.plugin.SlangSensor;
import org.sonarsource.slang.plugin.SlangTreeValidation.TokenValidationBuilder;

public class ScalaSensor extends SlangSensor {

  public static final Map<Class, Predicate<Token>> TOKEN_VALIDATION_MAP = new TokenValidationBuilder()
    .patternFor("package|\\{|}|;", PackageDeclarationTreeImpl.class)
    .patternFor("import|,|;", ImportDeclarationTreeImpl.class)
    .patternFor("def|\\(|,|\\)|\\[|\\]|implicit|:|=|;", FunctionDeclarationTreeImpl.class)
    // TODO Remove parentheses from BlockTree, see: whisk/utils/test/ExecutionContextFactoryTests.scala:39,40
    .patternFor("\\{|\\}|\\(|\\)|,|;", BlockTreeImpl.class)
    // TODO 0L should be IntegerLiteralTreeImpl instead of LiteralTreeImpl
    .patternFor("null|true|false|\\(|\\)|'.*|[0-9a-fA-FxX.eEfFdDlL+\\-]+", LiteralTreeImpl.class)
    .patternFor("\\(|\\)|\\+|-|[0-9a-fA-FxX]+[Ll]?", IntegerLiteralTreeImpl.class)
    .anyFor(IdentifierTreeImpl.class, StringLiteralTreeImpl.class, NativeTreeImpl.class)
    .build();

  private final Checks<SlangCheck> checks;

  public ScalaSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, ScalaLanguage language) {
    super(noSonarFilter, fileLinesContextFactory, language);
    checks = checkFactory.create(ScalaPlugin.SCALA_REPOSITORY_KEY);
    checks.addAnnotatedChecks((Iterable<?>) CheckList.scalaChecks());
  }

  @Override
  protected ASTConverter astConverter() {
    return new ScalaConverter();
  }

  @Override
  protected Map<Class, Predicate<Token>> tokenValidationMap() {
    return TOKEN_VALIDATION_MAP;
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
