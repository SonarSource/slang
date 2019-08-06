package org.sonarsource.slang.impl;

import org.junit.Test;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.MemberSelectTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThat;

public class MemberSelectTreeImplTest {

  @Test
  public void test() {
    TreeMetaData meta = null;
    IdentifierTree identifierTree = new IdentifierTreeImpl(meta, "y");
    Tree member = new IdentifierTreeImpl(meta, "x");
    MemberSelectTree memberSelect = new MemberSelectTreeImpl(meta, member, identifierTree);
    assertThat(memberSelect.children()).containsExactly(member, identifierTree);
    assertThat(memberSelect.expression()).isEqualTo(member);
    assertThat(memberSelect.identifier()).isEqualTo(identifierTree);
  }
}
