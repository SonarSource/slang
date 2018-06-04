package com.sonarsource.slang.checks;

import org.junit.Test;

public class TodoCommentCheckTest {

  @Test
  public void test() {
    Verifier.verify("TodoComment.slang", new TodoCommentCheck());
  }

}
