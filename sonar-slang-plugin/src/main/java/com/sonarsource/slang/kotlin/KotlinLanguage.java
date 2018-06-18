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

import org.sonar.api.config.Configuration;
import org.sonar.api.internal.apachecommons.lang.StringUtils;
import org.sonar.api.resources.AbstractLanguage;

public class KotlinLanguage extends AbstractLanguage {

  private Configuration configuration;

  public KotlinLanguage(Configuration configuration) {
    super(SlangPlugin.LANGUAGE_KEY, SlangPlugin.LANGUAGE_NAME);
    this.configuration = configuration;
  }

  @Override
  public String[] getFileSuffixes() {
    String[] suffixes = configuration.getStringArray(SlangPlugin.FILE_SUFFIXES_KEY);
    if (suffixes == null || suffixes.length == 0) {
      suffixes = StringUtils.split(SlangPlugin.FILE_SUFFIXES_DEFAULT_VALUE, ",");
    }
    return suffixes;
  }

}
