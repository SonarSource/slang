/*
 * SonarSource SLang
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.JumpTree;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.TreeMetaData;
import org.junit.Test;

import static com.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

public class JumpTreeImplTest {

  @Test
  public void test() {
    TreeMetaData meta = null;
    TokenImpl breakKeyword = new TokenImpl(new TextRangeImpl(1, 0, 1, 3), "break", Token.Type.KEYWORD);
    IdentifierTree labelBreak = new IdentifierTreeImpl(meta, "foo");
    JumpTreeImpl jumpTreeBreak = new JumpTreeImpl(meta, breakKeyword, JumpTree.JumpKind.BREAK, labelBreak);

    IdentifierTree labelContinue = new IdentifierTreeImpl(meta, "foo");
    TokenImpl continueKeyword = new TokenImpl(new TextRangeImpl(1, 0, 1, 3), "break", Token.Type.KEYWORD);
    JumpTreeImpl jumpTreeContinue = new JumpTreeImpl(meta, continueKeyword, JumpTree.JumpKind.CONTINUE, labelContinue);

    assertThat(jumpTreeBreak.children()).hasSize(1);
    assertThat(jumpTreeBreak.label().name()).isEqualTo("foo");
    assertThat(jumpTreeBreak.kind()).isEqualTo(JumpTree.JumpKind.BREAK);

    assertThat(jumpTreeContinue.children()).hasSize(1);
    assertThat(jumpTreeContinue.label().name()).isEqualTo("foo");
    assertThat(jumpTreeContinue.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE);

    assertThat(areEquivalent(jumpTreeBreak, new JumpTreeImpl(meta, breakKeyword, JumpTree.JumpKind.BREAK, labelBreak))).isTrue();
    assertThat(areEquivalent(jumpTreeBreak, jumpTreeContinue)).isFalse();
    assertThat(areEquivalent(jumpTreeBreak, new JumpTreeImpl(meta, breakKeyword, JumpTree.JumpKind.BREAK, new IdentifierTreeImpl(meta,"bar")))).isFalse();
    JumpTreeImpl jumpTree = new JumpTreeImpl(meta,breakKeyword, JumpTree.JumpKind.BREAK, null);

    assertThat(jumpTree.children()).isEmpty();
    assertThat(jumpTree.label()).isNull();

  }

}
