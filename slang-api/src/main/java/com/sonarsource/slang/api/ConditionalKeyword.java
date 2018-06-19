package com.sonarsource.slang.api;

public interface ConditionalKeyword {
  Token ifKeyword();
  Token thenKeyword();
  Token elseKeyword();
}
