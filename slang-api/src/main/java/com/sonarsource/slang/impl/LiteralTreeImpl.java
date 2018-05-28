package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.LiteralTree;

public class LiteralTreeImpl implements LiteralTree {

  private final String value;

  public LiteralTreeImpl(String value) {
    this.value = value;
  }

  @Override
  public String value() {
    return value;
  }

}
