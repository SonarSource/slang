/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.checks;

import org.junit.jupiter.api.Test;

class FileHeaderCheckTest {
  private FileHeaderCheck check = new FileHeaderCheck();
  @Test
  void test() {
    check.headerFormat = "// copyright 2018";
    Verifier.verify("fileheader/Noncompliant.slang", check);
    Verifier.verifyNoIssue("fileheader/Compliant.slang", check);
  }

  @Test
  void test_regex() {
    check.headerFormat = "// copyright 20\\d\\d";
    check.isRegularExpression = true;
    Verifier.verify("fileheader/Noncompliant.slang", check);
    Verifier.verifyNoIssue("fileheader/Compliant.slang", check);
  }
  @Test
  void test_multiline() {
    check.headerFormat = """
      /*
       * SonarSource SLang
       * Copyright (C) 1999-2001 SonarSource SA
       * mailto:info AT sonarsource DOT com
       */""";
    Verifier.verifyNoIssue("fileheader/Multiline.slang", check);
  }

  @Test
  void test_no_first_line() {
    check.headerFormat = "// copyright 20\\d\\d";
    check.isRegularExpression = true;
    Verifier.verify("fileheader/NoFirstLine.slang", check);
  }
}
