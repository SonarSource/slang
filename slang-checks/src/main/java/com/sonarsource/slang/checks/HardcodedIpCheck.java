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
package com.sonarsource.slang.checks;

import com.sonarsource.slang.api.StringLiteralTree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;

@Rule(key = "S1313")
public class HardcodedIpCheck implements SlangCheck {

  private static final String IP_ADDRESS_REGEX = "([^\\d.]*\\/)?(?<ip>(?:\\d{1,3}\\.){3}\\d{1,3}(?!\\d|\\.))(\\/.*)?";

  private static final Pattern pattern = Pattern.compile(IP_ADDRESS_REGEX);

  private static final String MESSAGE = "Make this IP \"{0}\" address configurable.";

  @Override
  public void initialize(InitContext init) {

    init.register(StringLiteralTree.class, (ctx, tree) -> {
      Matcher matcher = pattern.matcher(tree.content());
      if (matcher.matches()) {
        String ip = matcher.group("ip");
        if (areAllBelow256(ip.split("\\."))) {
          ctx.reportIssue(tree, MessageFormat.format(MESSAGE, ip));
        }
      }
    });

  }

  private static boolean areAllBelow256(String[] numbersAsStrings) {
    return Arrays.stream(numbersAsStrings).noneMatch(value -> Integer.valueOf(value) > 255);
  }
}
