/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
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
package org.sonarsource.slang.checks.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageTest {

  @Test
  void default_scala_function_name() {
    Pattern pattern = Pattern.compile(Language.SCALA_FUNCTION_OR_OPERATOR_NAMING_DEFAULT);
    assertThat(pattern.matcher("print").matches()).isTrue();
    assertThat(pattern.matcher("printLn").matches()).isTrue();
    assertThat(pattern.matcher("method_=").matches()).isTrue();
    assertThat(pattern.matcher("parse_!").matches()).isTrue();
    assertThat(pattern.matcher("+").matches()).isTrue();
    assertThat(pattern.matcher("<<").matches()).isTrue();
    assertThat(pattern.matcher("print_ln").matches()).isFalse();
    assertThat(pattern.matcher("PRINT").matches()).isFalse();
    assertThat(pattern.matcher("_print").matches()).isFalse();
    assertThat(pattern.matcher("+print").matches()).isFalse();
  }
  
  @Test
  void propertyDefaultValueTest() throws Exception {

    Field someString = LanguageTest.class.getDeclaredField("someString");
    Annotation[] annotations = someString.getAnnotations();
    
    assertThat(annotations).hasSize(1);

    PropertyDefaultValues defaultValues = someString.getAnnotation(PropertyDefaultValues.class);
    assertThat(defaultValues.value()).hasSize(3);

    assertThat(Arrays.stream(defaultValues.value()).map(PropertyDefaultValue::language).toList())
      .containsExactlyInAnyOrder(Language.GO, Language.RUBY, Language.SCALA);
    
    assertThat(Arrays.stream(defaultValues.value()).map(PropertyDefaultValue::defaultValue).toList())
      .containsExactlyInAnyOrder("go", "ruby", "scala");
    
  }
  
  @PropertyDefaultValue(language = Language.GO, defaultValue = "go")
  @PropertyDefaultValue(language = Language.SCALA, defaultValue = "scala")
  @PropertyDefaultValue(language = Language.RUBY, defaultValue = "ruby")
  private String someString = "";
}
