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
package com.sonarsource.slang.utils;

import com.sonarsource.slang.api.AssignmentExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.NativeKind;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static com.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static com.sonarsource.slang.utils.SyntacticEquivalence.findDuplicatedGroups;
import static org.assertj.core.api.Assertions.assertThat;

public class SyntacticEquivalenceTest {
  private static NativeKind KIND = new NativeKind() {
    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }
  };

  @Test
  public void test_equivalence() {
    Tree literal1 = literal("1");
    Tree literal2 = literal("2");
    assertThat(areEquivalent((Tree) null, null)).isTrue();
    assertThat(areEquivalent(literal1, null)).isFalse();
    assertThat(areEquivalent(null, literal1)).isFalse();
    assertThat(areEquivalent(literal1, literal1)).isTrue();
    assertThat(areEquivalent(literal1, literal("1"))).isTrue();
    assertThat(areEquivalent(literal1, literal2)).isFalse();

    Tree identifierA = identifier("a");
    assertThat(areEquivalent(identifierA, identifierA)).isTrue();
    assertThat(areEquivalent(identifierA, identifier("a"))).isTrue();
    assertThat(areEquivalent(identifierA, identifier("b"))).isFalse();
    assertThat(areEquivalent(identifierA, literal1)).isFalse();

    Tree binaryAEquals1 = binary(Operator.EQUAL_TO, identifierA, literal1);
    assertThat(areEquivalent(binaryAEquals1, binaryAEquals1)).isTrue();
    assertThat(areEquivalent(binaryAEquals1, binary(Operator.EQUAL_TO, identifierA, literal1))).isTrue();
    assertThat(areEquivalent(binaryAEquals1, binary(Operator.EQUAL_TO, identifierA, literal2))).isFalse();
    assertThat(areEquivalent(binaryAEquals1, binary(Operator.GREATER_THAN_OR_EQUAL_TO, identifierA, literal1))).isFalse();

    AssignmentExpressionTree.Operator plusEqualOperator = AssignmentExpressionTree.Operator.PLUS_EQUAL;
    Tree assignmentAPlusEqual1 = assignment(plusEqualOperator, identifierA, literal1);
    assertThat(areEquivalent(assignmentAPlusEqual1, assignmentAPlusEqual1)).isTrue();
    assertThat(areEquivalent(assignmentAPlusEqual1, assignment(plusEqualOperator, identifierA, literal1))).isTrue();
    assertThat(areEquivalent(assignmentAPlusEqual1, assignment(plusEqualOperator, identifierA, literal2))).isFalse();
    assertThat(areEquivalent(assignmentAPlusEqual1, assignment(AssignmentExpressionTree.Operator.TIMES_EQUAL, identifierA, literal1))).isFalse();
    assertThat(areEquivalent(assignmentAPlusEqual1, binaryAEquals1)).isFalse();

    Tree native1 = simpleNative(KIND, Collections.emptyList());
    assertThat(areEquivalent(native1, native1)).isTrue();
    assertThat(areEquivalent(native1, simpleNative(KIND, Collections.emptyList()))).isTrue();
    assertThat(areEquivalent(native1, simpleNative(KIND, Collections.singletonList(literal1)))).isFalse();
    assertThat(areEquivalent(native1, simpleNative(null, Collections.emptyList()))).isFalse();
    assertThat(areEquivalent(native1, literal1)).isFalse();
  }

  @Test
  public void test_equivalence_list() {
    List<Tree> list1 = Arrays.asList(identifier("a"), literal("2"));
    List<Tree> list2 = Arrays.asList(identifier("a"), literal("2"));
    List<Tree> list3 = Arrays.asList(identifier("a"), literal("3"));
    List<Tree> list4 = Collections.singletonList(identifier("a"));

    assertThat(areEquivalent((List<Tree>) null, null)).isTrue();
    assertThat(areEquivalent(list1, null)).isFalse();
    assertThat(areEquivalent(null, list1)).isFalse();
    assertThat(areEquivalent(list1, list1)).isTrue();
    assertThat(areEquivalent(list1, list2)).isTrue();
    assertThat(areEquivalent(list1, list3)).isFalse();
    assertThat(areEquivalent(list1, list4)).isFalse();
  }

  @Test
  public void duplicateGroups() {
    Tree a1 = identifier("a");
    Tree a2 = identifier("a");
    Tree a3 = a1;
    Tree b1 = identifier("b");
    assertThat(findDuplicatedGroups(Arrays.asList(a1, b1, a2, a3))).containsExactly(Arrays.asList(a1, a2, a3));
    assertThat(findDuplicatedGroups(Arrays.asList(a1, b1, null))).isEmpty();
  }

  private static Tree literal(String value) {
    return new LiteralTreeImpl(null, value);
  }

  private static Tree identifier(String name) {
    return new IdentifierTreeImpl(null, name);
  }

  private static Tree binary(Operator operator, Tree leftOperand, Tree rightOperand) {
    return new BinaryExpressionTreeImpl(null, operator, leftOperand, rightOperand);
  }

  private static Tree assignment(AssignmentExpressionTree.Operator operator, Tree leftOperand, Tree rightOperand) {
    return new AssignmentExpressionTreeImpl(null, operator, leftOperand, rightOperand);
  }

  private static Tree simpleNative(NativeKind kind, List<Tree> children) {
    return new NativeTreeImpl(null, kind, children);
  }

}
