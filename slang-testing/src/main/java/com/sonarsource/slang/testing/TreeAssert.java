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
package com.sonarsource.slang.testing;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import java.util.Objects;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class TreeAssert extends AbstractAssert<TreeAssert, Tree> {

  public TreeAssert(Tree actual) {
    super(actual, TreeAssert.class);
  }

  public TreeAssert isIdentifier(String expectedName) {
    isNotNull();
    isInstanceOf(IdentifierTree.class);
    IdentifierTree actualIdentifier = (IdentifierTree) actual;
    if (!Objects.equals(actualIdentifier.name(), expectedName)) {
      failWithMessage("Expected identifier's name to be <%s> but was <%s>", expectedName, actualIdentifier.name());
    }
    return this;
  }

  public TreeAssert isLiteral(String expected) {
    isNotNull();
    isInstanceOf(LiteralTree.class);
    LiteralTree actualLiteral = (LiteralTree) actual;
    if (!Objects.equals(actualLiteral.value(), expected)) {
      failWithMessage("Expected literal value to be <%s> but was <%s>", expected, actualLiteral.value());
    }
    return this;
  }

  public TreeAssert isBinaryExpression(BinaryExpressionTree.Operator expectedOperator) {
    isNotNull();
    isInstanceOf(BinaryExpressionTree.class);
    BinaryExpressionTree actualBinary = (BinaryExpressionTree) actual;
    if (!Objects.equals(actualBinary.operator(), expectedOperator)) {
      failWithMessage("Expected operator to be <%s> but was <%s>", expectedOperator, actualBinary.operator());
    }
    return this;
  }

  public TreeAssert isBlock(Class... classes) {
    isNotNull();
    isInstanceOf(BlockTree.class);
    hasChildren(classes);
    return this;
  }

  public TreeAssert hasChildren(Class... classes) {
    hasChildren(classes.length);
    for (int i = 0; i < actual.children().size(); i++) {
      Tree tree = actual.children().get(i);
      if (!classes[i].isAssignableFrom(tree.getClass())) {
        failWithMessage("Expected to find instance of <%s> but was <%s>", classes[i], tree.getClass());
      }
    }
    return this;
  }

  public TreeAssert hasChildren(int count) {
    isNotNull();
    if (actual.children().size() != count) {
      failWithMessage("Expected to have <%s> children elements but found <%s>", count, actual.children().size());
    }
    return this;
  }

  public TreeAssert hasTextRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    isNotNull();
    TextRange range = actual.metaData().textRange();
    assertThat(range.start().line()).isEqualTo(startLine);
    assertThat(range.start().lineOffset()).isEqualTo(startLineOffset);
    assertThat(range.end().line()).isEqualTo(endLine);
    assertThat(range.end().lineOffset()).isEqualTo(endLineOffset);
    return this;
  }

  public static TreeAssert assertTree(Tree actual) {
    return new TreeAssert(actual);
  }

}
