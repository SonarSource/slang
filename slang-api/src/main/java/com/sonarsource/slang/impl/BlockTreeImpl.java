package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import java.util.List;

public class BlockTreeImpl extends BaseTreeImpl implements BlockTree {

  private final List<Tree> statementOrExpressions;

  public BlockTreeImpl(TextRange textRange, List<Tree> statementOrExpressions) {
    super(textRange);
    this.statementOrExpressions = statementOrExpressions;
  }

  @Override
  public List<Tree> statementOrExpressions() {
    return statementOrExpressions;
  }

  @Override
  public List<Tree> children() {
    return statementOrExpressions();
  }
}
