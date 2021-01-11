/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

public class ScalaLanguage extends AbstractLanguage {

  private Configuration configuration;

  public ScalaLanguage(Configuration configuration) {
    super(ScalaPlugin.SCALA_LANGUAGE_KEY, ScalaPlugin.SCALA_LANGUAGE_NAME);
    this.configuration = configuration;
  }

  @Override
  public String[] getFileSuffixes() {
    String[] suffixes = configuration.getStringArray(ScalaPlugin.SCALA_FILE_SUFFIXES_KEY);
    if (suffixes.length == 0) {
      suffixes = ScalaPlugin.SCALA_FILE_SUFFIXES_DEFAULT_VALUE.split(",");
    }
    return suffixes;
  }

}
