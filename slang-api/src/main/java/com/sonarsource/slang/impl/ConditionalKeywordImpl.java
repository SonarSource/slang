package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.ConditionalKeyword;
import com.sonarsource.slang.api.Token;

public class ConditionalKeywordImpl implements ConditionalKeyword {
  private final Token ifKeyword;
  private final Token thenKeyword;
  private final Token elseKeyword;

  public ConditionalKeywordImpl(Token ifKeyword, Token thenKeyword, Token elseKeyword) {
    this.ifKeyword = ifKeyword;
    this.thenKeyword = thenKeyword;
    this.elseKeyword = elseKeyword;
  }

  @Override
  public Token ifKeyword() {
    return ifKeyword;
  }

  @Override
  public Token thenKeyword() {
    return thenKeyword;
  }

  @Override
  public Token elseKeyword() {
    return elseKeyword;
  }
}
