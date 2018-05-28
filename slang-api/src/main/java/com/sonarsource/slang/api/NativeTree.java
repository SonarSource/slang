package com.sonarsource.slang.api;

import java.util.List;

public interface NativeTree {

  NativeKind nativeKind();

  List<Tree> children();

}
