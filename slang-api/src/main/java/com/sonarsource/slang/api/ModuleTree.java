package com.sonarsource.slang.api;

import java.util.List;

/**
 * SLang module unit.
 */
public interface ModuleTree extends Tree {
  List<Tree> types();
}
