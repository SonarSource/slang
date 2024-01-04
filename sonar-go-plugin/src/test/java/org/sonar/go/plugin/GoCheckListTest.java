/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonar.go.plugin;

import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.testing.PackageScanner;

import static org.assertj.core.api.Java6Assertions.assertThat;

class GoCheckListTest {

  private static final String GO_CHECKS_PACKAGE = "org.sonar.go.checks";

  @Test
  void go_checks_size() {
    Assertions.assertThat(GoCheckList.checks()).hasSize(38);
  }

  @Test
  void go_specific_checks_are_added_to_check_list() {
    List<String> checkListNames = GoCheckList.checks().stream().map(Class::getName).collect(Collectors.toList());
    List<String> languageImplementation = PackageScanner.findSlangChecksInPackage(GO_CHECKS_PACKAGE);
    for (String languageCheck : languageImplementation) {
      assertThat(checkListNames).contains(languageCheck);
      assertThat(languageCheck).endsWith("GoCheck");
    }
  }

  @Test
  void go_excluded_not_present() {
    List<Class<?>> checks = GoCheckList.checks();
    for (Class excluded : GoCheckList.GO_CHECK_BLACK_LIST) {
      assertThat(checks).doesNotContain(excluded);
    }
  }
}
