package org.sonarsource.slang.api;

public interface MemberSelectTree extends Tree {

  Tree expression();

  IdentifierTree identifier();
}
