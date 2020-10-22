package org.sonarsource.slang.impl;

import java.util.Collections;
import java.util.List;
import org.sonarsource.slang.api.PlaceHolderTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

public class PlaceHolderTreeImpl extends IdentifierTreeImpl implements PlaceHolderTree {
  private final Token placeHolderToken;

  public PlaceHolderTreeImpl(TreeMetaData metaData, Token placeHolderToken) {
    super(metaData, "_");
    this.placeHolderToken = placeHolderToken;
  }

  @Override
  public Token placeHolderToken() {
    return placeHolderToken;
  }

  @Override
  public List<Tree> children() {
    return Collections.emptyList();
  }
}
