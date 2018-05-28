package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.NativeKind;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.Tree;
import java.util.List;

public class NativeTreeImpl implements NativeTree {

  private final NativeKind nativeKind;
  private final List<Tree> children;

  public NativeTreeImpl(NativeKind nativeKind, List<Tree> children) {
    this.nativeKind = nativeKind;
    this.children = children;
  }

  @Override
  public NativeKind nativeKind() {
    return nativeKind;
  }

  @Override
  public List<Tree> children() {
    return children;
  }
}
