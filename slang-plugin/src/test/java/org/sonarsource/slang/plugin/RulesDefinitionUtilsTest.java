/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.checks.utils.Language;
import org.sonarsource.slang.checks.utils.PropertyDefaultValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RulesDefinitionUtilsTest {

  private static final String REPOSITORY = "test";
  private RulesDefinition.NewRepository repository;
  private RulesDefinition.Context context;

  @Test
  void test_setDefaultValuesForParameters_ruby() {
    initRepository();

    RulesDefinitionUtils.setDefaultValuesForParameters(repository, Collections.singletonList(Check.class), Language.RUBY);
    repository.done();

    RulesDefinition.Repository repository = context.repository(REPOSITORY);
    RulesDefinition.Rule check = repository.rule("check");
    RulesDefinition.Param param = check.param("param");
    assertThat(param.defaultValue()).isEqualTo("ruby");
  }

  @Test
  void test_setDefaultValuesForParameters_scala() {
    initRepository();

    RulesDefinitionUtils.setDefaultValuesForParameters(repository, Collections.singletonList(Check.class), Language.SCALA);
    repository.done();

    RulesDefinition.Repository repository = context.repository(REPOSITORY);
    RulesDefinition.Rule check = repository.rule("check");
    RulesDefinition.Param param = check.param("param");
    assertThat(param.defaultValue()).isEqualTo("scala");
  }

  @Test
  void wrong_annotation() {
    context = new RulesDefinition.Context();
    repository = context.createRepository(REPOSITORY, Language.SCALA.toString());
    new RulesDefinitionAnnotationLoader().load(repository, WrongAnnotationUsage.class);

    List<Class<?>> wrongAnnotationCheck = Collections.singletonList(WrongAnnotationUsage.class);
    assertThatThrownBy( () -> RulesDefinitionUtils.setDefaultValuesForParameters(repository, wrongAnnotationCheck, Language.RUBY))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Invalid @PropertyDefaultValue on WrongAnnotationUsage for language RUBY");

    assertThatThrownBy( () -> RulesDefinitionUtils.setDefaultValuesForParameters(repository, wrongAnnotationCheck, Language.SCALA))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Invalid @PropertyDefaultValue on WrongAnnotationUsage for language SCALA");
  }

  private void initRepository() {
    context = new RulesDefinition.Context();
    repository = context.createRepository(REPOSITORY, Language.SCALA.toString());
    new RulesDefinitionAnnotationLoader().load(repository, Check.class);
  }

  @Rule(key = "check", name = "Check", description = "Desc")
  static class Check {

    @RuleProperty(key = "param")
    @PropertyDefaultValue(language = Language.RUBY, defaultValue = "ruby")
    @PropertyDefaultValue(language = Language.SCALA, defaultValue = "scala")
    String param;

    String notAParamField;

    @RuleProperty(key = "paramNoDefault")
    String paramNoDefault;
  }

  @Rule(key = "invalid", name = "Check", description = "Desc")
  static class WrongAnnotationUsage {

    @RuleProperty(key = "param")
    @PropertyDefaultValue(language = Language.SCALA, defaultValue = "scala")
    @PropertyDefaultValue(language = Language.SCALA, defaultValue = "ruby")
    String param;
  }
}
