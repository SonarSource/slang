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

import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.BlockTreeImpl;
import com.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.IfTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.MatchCaseTreeImpl;
import com.sonarsource.slang.impl.MatchTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import com.sonarsource.slang.impl.TextPointerImpl;
import com.sonarsource.slang.impl.TextRangeImpl;
import com.sonarsource.slang.impl.TopLevelTreeImpl;
import com.sonarsource.slang.impl.TreeMetaDataProvider;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.lexer.KtSingleValueToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtWhenCondition;
import org.jetbrains.kotlin.psi.KtWhenEntry;
import org.jetbrains.kotlin.psi.KtWhenExpression;

class KotlinTreeVisitor {
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

  private final Document psiDocument;
  private final TreeMetaDataProvider metaDataProvider;
  private final Tree sLangAST;

  public KotlinTreeVisitor(PsiFile psiFile) {
    this.psiDocument = psiFile.getViewProvider().getDocument();
    metaDataProvider = new TreeMetaDataProvider(Collections.emptyList());
    this.sLangAST = createElement(psiFile);
  }

  @CheckForNull
  private Tree createElement(@Nullable PsiElement element) {
    if (element == null) {
      return null;
    }

    TreeMetaData metaData = getTreeMetaData(element);

    if (element instanceof PsiErrorElement) {
      throw new IllegalStateException("Cannot createElement for a PsiErrorElement");
    } else if (element instanceof PsiWhiteSpace || element instanceof LeafPsiElement) {
      // skip tokens and whitespaces nodes in kotlin AST
      return null;
    } else if (element instanceof KtBinaryExpression) {
      return createBinaryExpression((KtBinaryExpression) element, metaData);
    } else if (element instanceof KtNameReferenceExpression) {
      return new IdentifierTreeImpl(metaData, element.getText());
    } else if (element instanceof KtConstantExpression) {
      return new LiteralTreeImpl(metaData, element.getText());
    } else if (element instanceof KtBlockExpression) {
      List<Tree> statementOrExpressions = ((KtBlockExpression) element).getStatements().stream().map(this::createElement).collect(Collectors.toList());
      return new BlockTreeImpl(metaData, statementOrExpressions);
    } else if (element instanceof KtFile) {
      return new TopLevelTreeImpl(metaData, getChildrenTree(element), Collections.emptyList());
    } else if (element instanceof KtFunction) {
      KtFunction functionElement = (KtFunction) element;
      List<Tree> modifiers = Collections.emptyList();
      // FIXME modifiers and return type
      Tree returnType = null;
      PsiElement nameIdentifier = functionElement.getNameIdentifier();
      IdentifierTree identifierTree = null;
      if (nameIdentifier != null) {
        identifierTree = new IdentifierTreeImpl(getTreeMetaData(nameIdentifier), functionElement.getName());
      }
      List<Tree> parametersList = functionElement.getValueParameters().stream()
        .map(this::createElement)
        .collect(Collectors.toList());
      Tree bodyTree = createElement(functionElement.getBodyExpression());
      if (bodyTree != null && !(bodyTree instanceof BlockTree)) {
        // FIXME are we sure we want body of function as block tree ?
        bodyTree = new BlockTreeImpl(bodyTree.metaData(), Collections.singletonList(bodyTree));
      }
      if (bodyTree != null && bodyTree.children().isEmpty()) {
        bodyTree = null;
      }
      return new FunctionDeclarationTreeImpl(metaData, modifiers, returnType, identifierTree, parametersList, (BlockTree) bodyTree);
    } else if (element instanceof KtIfExpression) {
      KtIfExpression ifElement = (KtIfExpression) element;
      Tree condition = createElement(ifElement.getCondition());
      Tree thenBranch = createElement(ifElement.getThen());
      Tree elseBranch = createElement(ifElement.getElse());
      return new IfTreeImpl(metaData, condition, thenBranch, elseBranch);
    } else if (element instanceof KtWhenExpression) {
      KtWhenExpression whenElement = (KtWhenExpression) element;
      Tree subjectExpression = createElement(whenElement.getSubjectExpression());
      List<MatchCaseTree> whenExpressions = whenElement.getEntries().stream()
        .map(this::createElement)
        .map(MatchCaseTree.class::cast)
        .collect(Collectors.toList());
      return new MatchTreeImpl(metaData, subjectExpression, whenExpressions);
    } else if (element instanceof KtWhenEntry) {
      KtWhenEntry whenElement = (KtWhenEntry) element;
      Tree conditions = null;
      Tree body = createElement(whenElement.getExpression());
      if (!whenElement.isElse()) {
        List<Tree> conditionsList = Arrays.stream(whenElement.getConditions()).map(this::createElement).collect(Collectors.toList());
        TextPointer startPointer = conditionsList.get(0).metaData().textRange().start();
        TextPointer endPointer = conditionsList.get(conditionsList.size() - 1).metaData().textRange().end();
        TextRange textRange = new TextRangeImpl(startPointer, endPointer);
        TreeMetaData treeMetaData = metaDataProvider.metaData(textRange);
        conditions = new NativeTreeImpl(treeMetaData, new KotlinNativeKind(KtWhenCondition.class), conditionsList);
      }
      return new MatchCaseTreeImpl(getTreeMetaData(whenElement), conditions, body);
    } else if (element instanceof KtStringTemplateExpression) {
      KtStringTemplateEntry[] entries = ((KtStringTemplateExpression) element).getEntries();
      if (entries.length == 1 && entries[0] instanceof KtLiteralStringTemplateEntry) {
        // Non-template strings, ie. not in the form "string ${1 + 1}"
        return new LiteralTreeImpl(metaData, '\"' + entries[0].getText() + '\"');
      } else {
        return new NativeTreeImpl(metaData, new KotlinNativeKind(element), getChildrenTree(element));
      }
    } else {
      return new NativeTreeImpl(metaData, new KotlinNativeKind(element), getChildrenTree(element));
    }
  }

  @NotNull
  private Tree createBinaryExpression(@NotNull KtBinaryExpression element, TreeMetaData metaData) {
    Tree leftOperand = createElement(element.getLeft());
    Tree rightOperand = createElement(element.getRight());
    Operator operator = TOKENS_OPERATOR_MAP.get(element.getOperationToken());
    if (operator != null) {
      return new BinaryExpressionTreeImpl(metaData, operator, leftOperand, rightOperand);
    } else {
      // FIXME ensure they are all supported. Ex: Add EQ for assignments
      return new NativeTreeImpl(
        metaData,
        new KotlinNativeKind(element, element.getOperationReference().getReferencedNameElement().getText()),
        Arrays.asList(leftOperand, rightOperand));
    }
  }

  private TreeMetaData getTreeMetaData(@NotNull PsiElement element) {
    TextPointerImpl startPointer = textPointerAtOffset(psiDocument, element.getTextRange().getStartOffset());
    TextPointerImpl endPointer = textPointerAtOffset(psiDocument, element.getTextRange().getEndOffset());
    TextRange textRange = new TextRangeImpl(startPointer, endPointer);
    return metaDataProvider.metaData(textRange);
  }

  private List<Tree> getChildrenTree(@NotNull PsiElement element) {
    return Arrays.stream(element.getChildren())
      .map(this::createElement)
      .collect(Collectors.toList());
  }

  @NotNull
  private static TextPointerImpl textPointerAtOffset(Document psiDocument, int startOffset) {
    int startLineNumber = psiDocument.getLineNumber(startOffset);
    int startLineNumberOffset = psiDocument.getLineStartOffset(startLineNumber);
    int startLineOffset = startOffset - startLineNumberOffset;
    return new TextPointerImpl(startLineNumber + 1, startLineOffset);
  }

  Tree getSLangAST() {
    return sLangAST;
  }
}
