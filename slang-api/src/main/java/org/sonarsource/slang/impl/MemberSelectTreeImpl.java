package org.sonarsource.slang.impl;

import java.util.ArrayList;
import java.util.List;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.MemberSelectTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

public class MemberSelectTreeImpl extends BaseTreeImpl implements MemberSelectTree {

  private final Tree expression;
  private final IdentifierTree identifier;

  public MemberSelectTreeImpl(TreeMetaData metaData, Tree expression, IdentifierTree identifier) {
    super(metaData);
    this.expression = expression;
    this.identifier = identifier;
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(expression);
    children.add(identifier);
    return children;
  }
}
