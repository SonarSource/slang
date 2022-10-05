package org.sonar.go.checks;

import org.junit.jupiter.api.Test;

class DuplicateBranchGoCheckTest {
  @Test
  void test() {
    GoVerifier.verify("DuplicateBranchGoCheck.go", new DuplicateBranchGoCheck());
  }
}