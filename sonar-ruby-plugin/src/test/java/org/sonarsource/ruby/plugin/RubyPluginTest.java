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
package org.sonarsource.ruby.plugin;

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class RubyPluginTest {

  private static final Version VERSION_6_7 = Version.create(6, 7);
  private static final Version VERSION_7_2 = Version.create(7, 2);
  private RubyPlugin rubyPlugin = new RubyPlugin();

  @Test
  public void sonarqube_6_7_extensions() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(VERSION_6_7, SonarQubeSide.SERVER);
    Plugin.Context context = new Plugin.Context(runtime);
    rubyPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(9);
  }

  @Test
  public void sonarqube_7_2_extensions() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(VERSION_7_2, SonarQubeSide.SERVER);
    Plugin.Context context = new Plugin.Context(runtime);
    rubyPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(10);
  }

}
