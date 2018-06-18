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

import com.sonarsource.slang.api.AssignmentExpressionTree;
import com.sonarsource.slang.api.ParameterTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CatchTreeImplTest {

  @Test
  public void test() {
    TreeMetaData meta = null;
    ParameterTree parameter = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta,"e"), null);
    Tree lhs = new IdentifierTreeImpl(meta, "x");
    Tree one = new LiteralTreeImpl(meta, "1");
    Tree assignmentExpressionTree =
        new AssignmentExpressionTreeImpl(meta, AssignmentExpressionTree.Operator.EQUAL, lhs, one);
    CatchTreeImpl catchWithIdentifier = new CatchTreeImpl(meta, parameter, assignmentExpressionTree);
    CatchTreeImpl catchWithoutIdentifier = new CatchTreeImpl(meta, null, assignmentExpressionTree);

    assertThat(catchWithIdentifier.children()).containsExactly(parameter, assignmentExpressionTree);
    assertThat(catchWithIdentifier.catchParameter()).isEqualTo(parameter);
    assertThat(catchWithIdentifier.catchBlock()).isEqualTo(assignmentExpressionTree);

    assertThat(catchWithoutIdentifier.children()).containsExactly(assignmentExpressionTree);
    assertThat(catchWithoutIdentifier.catchParameter()).isNull();
    assertThat(catchWithoutIdentifier.catchBlock()).isEqualTo(assignmentExpressionTree);
  }
}
