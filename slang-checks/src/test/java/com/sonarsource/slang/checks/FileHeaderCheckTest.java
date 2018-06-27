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

import org.junit.Test;

public class FileHeaderCheckTest {
  FileHeaderCheck check = new FileHeaderCheck();
  @Test
  public void test() {
    check.headerFormat = "// copyright 2018";
    Verifier.verify("FileHeader_nok.slang", check);
    Verifier.verifyNoIssue("FileHeader_ok.slang", check);
  }

  @Test
  public void test_regex() {
    check.headerFormat = "// copyright 20\\d\\d";
    check.isRegularExpression = true;
    Verifier.verify("FileHeader_nok.slang", check);
    Verifier.verifyNoIssue("FileHeader_ok.slang", check);
  }
  @Test
  public void test_multiline() {
    check.headerFormat = "/*\n" +
      " * SonarSource SLang\n" +
      " * Copyright (C) 2009-2018 SonarSource SA\n" +
      " * mailto:info AT sonarsource DOT com\n" +
      " *\n" +
      " * This program is free software; you can redistribute it and/or\n" +
      " * modify it under the terms of the GNU Lesser General Public\n" +
      " * License as published by the Free Software Foundation; either\n" +
      " * version 3 of the License, or (at your option) any later version.\n" +
      " *\n" +
      " * This program is distributed in the hope that it will be useful,\n" +
      " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
      " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU\n" +
      " * Lesser General Public License for more details.\n" +
      " *\n" +
      " * You should have received a copy of the GNU Lesser General Public License\n" +
      " * along with this program; if not, write to the Free Software Foundation,\n" +
      " * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.\n" +
      " */";
    Verifier.verifyNoIssue("FileHeader_multiline.slang", check);
  }
}
