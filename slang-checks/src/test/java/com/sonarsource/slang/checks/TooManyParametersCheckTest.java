package com.sonarsource.slang.checks;

import org.junit.Test;

public class TooManyParametersCheckTest {

  @Test
  public void test() {
    Verifier.verify("TooManyParameters.slang", new TooManyParametersCheck());
  }

}
