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

import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.testing.PackageScanner;

import static org.assertj.core.api.Java6Assertions.assertThat;

class ScalaCheckListTest {

  private static final String SCALA_CHECKS_PACKAGE = "org.sonarsource.scala.checks";

  @Test
  void scala_checks_size() {
    Assertions.assertThat(ScalaCheckList.checks()).hasSizeGreaterThanOrEqualTo(40);
  }

  @Test
  void scala_specific_checks_are_added_to_check_list() {
    List<String> languageImplementation = PackageScanner.findSlangChecksInPackage(SCALA_CHECKS_PACKAGE);

    List<String> checkListNames = ScalaCheckList.checks().stream().map(Class::getName).collect(Collectors.toList());
    List<String> scalaSpecificChecks = ScalaCheckList.SCALA_LANGUAGE_SPECIFIC_CHECKS.stream().map(Class::getName).collect(Collectors.toList());

    for (String languageCheck : languageImplementation) {
      assertThat(checkListNames).contains(languageCheck);
      assertThat(scalaSpecificChecks).contains(languageCheck);
      assertThat(languageCheck).endsWith("ScalaCheck");
    }
  }

  @Test
  void scala_excluded_not_present() {
    List<Class<?>> checks = ScalaCheckList.checks();
    for (Class excluded : ScalaCheckList.SCALA_CHECK_BLACK_LIST) {
      assertThat(checks).doesNotContain(excluded);
    }
  }

  @Test
  void scala_included_are_present() {
    List<Class<?>> checks = ScalaCheckList.checks();
    for (Class specificCheck : ScalaCheckList.SCALA_LANGUAGE_SPECIFIC_CHECKS) {
      assertThat(checks).contains(specificCheck);
    }
  }
}
