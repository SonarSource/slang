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
package org.sonarsource.ruby.plugin;

import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonarsource.slang.testing.PackageScanner;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class RubyCheckListTest {

  private static final String RUBY_CHECKS_PACKAGE = "org.sonarsource.ruby.checks";

  @Test
  public void ruby_checks_size() {
    Assertions.assertThat(RubyCheckList.checks().size()).isGreaterThanOrEqualTo(40);
  }

  @Test
  public void ruby_specific_checks_are_added_to_check_list() {
    List<String> languageImplementation = PackageScanner.findSlangChecksInPackage(RUBY_CHECKS_PACKAGE);

    List<String> checkListNames = RubyCheckList.checks().stream().map(Class::getName).collect(Collectors.toList());
    List<String> rubySpecificChecks = RubyCheckList.RUBY_SPECIFIC_CHECKS.stream().map(Class::getName).collect(Collectors.toList());

    for (String languageCheck : languageImplementation) {
      assertThat(checkListNames).contains(languageCheck);
      assertThat(rubySpecificChecks).contains(languageCheck);
      assertThat(languageCheck).endsWith("RubyCheck");
    }
  }

  @Test
  public void ruby_excluded_not_present() {
    List<Class<?>> checks = RubyCheckList.checks();
    for (Class excluded : RubyCheckList.RUBY_CHECK_BLACK_LIST) {
      assertThat(checks).doesNotContain(excluded);
    }
  }
}
