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
package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.NativeKind;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import com.sonarsource.slang.impl.TextPointerImpl;
import com.sonarsource.slang.impl.TextRangeImpl;
import com.sonarsource.slang.impl.TreeMetaDataProvider;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtSingleValueToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;

class KotlinTreeVisitor extends KtTreeVisitorVoid {
  private static final NativeKind GENERIC_NATIVE_KIND = new NativeKind() {
  };
  private static final Map<KtSingleValueToken, Operator> TOKENS_OPERATOR_MAP = Collections.unmodifiableMap(Stream.of(
    new SimpleEntry<>(KtTokens.EQEQ, Operator.EQUAL_TO),
    new SimpleEntry<>(KtTokens.EXCLEQ, Operator.NOT_EQUAL_TO),
    new SimpleEntry<>(KtTokens.LT, Operator.LESS_THAN),
    new SimpleEntry<>(KtTokens.GT, Operator.GREATER_THAN),
    new SimpleEntry<>(KtTokens.LTEQ, Operator.LESS_THAN_OR_EQUAL_TO),
    new SimpleEntry<>(KtTokens.GTEQ, Operator.GREATER_THAN_OR_EQUAL_TO),
    new SimpleEntry<>(KtTokens.OROR, Operator.CONDITIONAL_OR),
    new SimpleEntry<>(KtTokens.ANDAND, Operator.CONDITIONAL_AND),
    new SimpleEntry<>(KtTokens.PLUS, Operator.PLUS),
    new SimpleEntry<>(KtTokens.MINUS, Operator.MINUS),
    new SimpleEntry<>(KtTokens.MUL, Operator.TIMES),
    new SimpleEntry<>(KtTokens.DIV, Operator.DIVIDED_BY))
    .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

  private List<Tree> currentChildren = new ArrayList<>();
  private final TreeMetaDataProvider metaDataProvider;
  private final Document psiDocument;

  public KotlinTreeVisitor(PsiFile psiFile) {
    this.psiDocument = psiFile.getViewProvider().getDocument();
    this.metaDataProvider = new TreeMetaDataProvider(Collections.emptyList());
  }

  public void visitElement(@NotNull PsiElement element) {
    List<Tree> siblings = currentChildren;
    currentChildren = new LinkedList<>();
    super.visitElement(element);
    Tree sLangElement = createElement(element, currentChildren);
    currentChildren = siblings;
    if (sLangElement != null) {
      currentChildren.add(sLangElement);
    }
  }

  private Tree createElement(PsiElement element, List<Tree> children) {
    TreeMetaData metaData = getTreeMetaData(element);
    if (element instanceof PsiWhiteSpace || element instanceof LeafPsiElement) {
      // skip tokens and whitespaces nodes in kotlin AST
      return null;
    } else if (element instanceof KtBinaryExpression) {
      BinaryExpressionTree.Operator operator = mapBinaryExpression(((KtBinaryExpression) element).getOperationToken());
      // child at index 1 is the KtOperationReferenceExpression
      return new BinaryExpressionTreeImpl(metaData, operator, children.get(0), children.get(2));
    } else if (element instanceof KtNameReferenceExpression) {
      return new IdentifierTreeImpl(metaData, element.getText());
    } else if (element instanceof KtConstantExpression) {
      return new LiteralTreeImpl(metaData, element.getText());
    } else {
      return new NativeTreeImpl(metaData, GENERIC_NATIVE_KIND, children);
    }
  }

  private static BinaryExpressionTree.Operator mapBinaryExpression(IElementType operationTokenType) {
    Operator operator = TOKENS_OPERATOR_MAP.get(operationTokenType);
    if (operator == null) {
      throw new IllegalStateException("Binary operation type not supported: " + operationTokenType);
    }
    return operator;
  }

  private TreeMetaData getTreeMetaData(@NotNull PsiElement element) {
    TextPointerImpl startPointer = textPointerAtOffset(psiDocument, element.getTextRange().getStartOffset());
    TextPointerImpl endPointer = textPointerAtOffset(psiDocument, element.getTextRange().getEndOffset());
    TextRange textRange = new TextRangeImpl(startPointer, endPointer);
    return metaDataProvider.metaData(textRange);
  }

  @NotNull
  private static TextPointerImpl textPointerAtOffset(Document psiDocument, int startOffset) {
    int startLineNumber = psiDocument.getLineNumber(startOffset);
    int startLineNumberOffset = psiDocument.getLineStartOffset(startLineNumber);
    int startLineOffset = startOffset - startLineNumberOffset;
    return new TextPointerImpl(startLineNumber + 1, startLineOffset);
  }

  Tree getSLangAST() {
    return currentChildren.get(0);
  }
}
