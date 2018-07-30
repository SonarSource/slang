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
package org.sonarsource.slang;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  DuplicationsTest.class,
  ExternalReportTest.class,
  FileSuffixesTest.class,
  MeasuresTest.class,
})
public class Tests {

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(getSlangPluginLocation())
    .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
    .restoreProfileAtStartup(FileLocation.of("src/test/resources/nosonar.xml"))
    .restoreProfileAtStartup(FileLocation.of("src/test/resources/norule.xml"))
    .build();

  private static Location getSlangPluginLocation() {
    String slangVersion = System.getProperty("slangVersion");
    if (StringUtils.isEmpty(slangVersion)) {
      // use the plugin that was built on local machine
      return FileLocation.byWildcardMavenFilename(new File("../../sonar-kotlin-plugin/target"), "sonar-kotlin-plugin-*.jar");
    } else {
      // QA environment downloads the plugin built by the CI job
      return MavenLocation.of("org.sonarsource.slang", "sonar-kotlin-plugin", slangVersion);
    }
  }

}
