package com.sonarsource.slang.api;

import java.util.List;

public interface BlockTree extends Tree {

  List<Tree> statementOrExpressions();

}
