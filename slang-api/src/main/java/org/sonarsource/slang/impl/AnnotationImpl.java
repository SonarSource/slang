/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource Sàrl
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
package org.sonarsource.slang.impl;

import java.util.List;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.TextRange;

public class AnnotationImpl implements Annotation {

  private final String shortName;
  private final List<String> argumentsText;
  private final TextRange range;

  public AnnotationImpl(String shortName, List<String> argumentsText, TextRange range) {
    this.shortName = shortName;
    this.argumentsText = argumentsText;
    this.range = range;
  }

  @Override
  public String shortName() {
    return shortName;
  }

  @Override
  public List<String> argumentsText() {
    return argumentsText;
  }

  @Override
  public TextRange textRange() {
    return range;
  }
}
