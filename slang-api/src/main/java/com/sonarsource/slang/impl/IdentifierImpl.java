package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.IdentifierTree;

public class IdentifierImpl implements IdentifierTree {

  private final String name;

  public IdentifierImpl(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }
}
