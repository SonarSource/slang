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
package org.sonarsource.slang.checks;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonCheckListTest {

  // TODO: Add logic for rules that require language specific configuration at construction time
  private static List<String> CHECKS_WITH_CONFIG = Collections.singletonList("CommentedCodeCheck.java");
  @Test
  public void all_checks_should_be_present() {
    File directory = new File("src/main/java/org/sonarsource/slang/checks");
    File[] checkFiles = directory.listFiles((dir, name) ->
        name.endsWith("Check.java") && !name.startsWith("Abstract") && !CHECKS_WITH_CONFIG.contains(name));
    assertThat(CommonCheckList.allChecks().size()).isEqualTo(checkFiles.length);
  }

  @Test
  public void each_check_has_to_be_used() {
    HashSet<Class> allChecks = new HashSet<>(CommonCheckList.allChecks());
    allChecks.removeAll(CommonCheckList.kotlinChecks());
    allChecks.removeAll(CommonCheckList.rubyChecks());
    assertThat(allChecks).isEmpty();
  }

}
