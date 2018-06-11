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

import com.sonarsource.slang.api.AssignmentExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree.Operator;
import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.NativeKind;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import com.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.BlockTreeImpl;
import com.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.IfTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.MatchCaseTreeImpl;
import com.sonarsource.slang.impl.MatchTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import com.sonarsource.slang.impl.TextRangeImpl;
import com.sonarsource.slang.impl.TopLevelTreeImpl;
import com.sonarsource.slang.impl.TreeMetaDataProvider;
import com.sonarsource.slang.kotlin.utils.KotlinTextRanges;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtOperationExpression;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtTypeElement;
import org.jetbrains.kotlin.psi.KtWhenCondition;
import org.jetbrains.kotlin.psi.KtWhenEntry;
import org.jetbrains.kotlin.psi.KtWhenExpression;

class KotlinTreeVisitor {
  // Native kind for trees that can be null in kotlin, but not in Slang
  private static final NativeKind NULL_NATIVE_KIND = new KotlinNativeKind(PsiElement.class, "NULL_NATIVE");
  private static final Map<KtToken, Operator> TOKENS_OPERATOR_MAP = Collections.unmodifiableMap(Stream.of(
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

  private static final Map<KtToken, AssignmentExpressionTree.Operator> ASSIGNMENTS_OPERATOR_MAP = Collections.unmodifiableMap(Stream.of(
    new SimpleEntry<>(KtTokens.EQ, AssignmentExpressionTree.Operator.EQUAL),
    new SimpleEntry<>(KtTokens.PLUSEQ, AssignmentExpressionTree.Operator.PLUS_EQUAL),
    new SimpleEntry<>(KtTokens.MINUSEQ, AssignmentExpressionTree.Operator.MINUS_EQUAL),
    new SimpleEntry<>(KtTokens.MULTEQ, AssignmentExpressionTree.Operator.TIMES_EQUAL),
    new SimpleEntry<>(KtTokens.PERCEQ, AssignmentExpressionTree.Operator.MODULO_EQUAL))
    // FIXME missing '/=' operator in grammar
    .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

  private final Document psiDocument;
  private final TreeMetaDataProvider metaDataProvider;
  private final Tree sLangAST;

  public KotlinTreeVisitor(PsiFile psiFile, TreeMetaDataProvider metaDataProvider) {
    this.psiDocument = psiFile.getViewProvider().getDocument();
    this.metaDataProvider = metaDataProvider;
    this.sLangAST = createElement(psiFile);
    if (this.sLangAST == null) {
      throw new ParseException("Slang AST should never be null");
    }
  }

  @CheckForNull
  private Tree createElement(@Nullable PsiElement element) {
    if (element == null || shouldSkipElement(element)) {
      // skip tokens and whitespaces nodes in kotlin AST
      return null;
    }
    return convertElementToSlangAST(element, getTreeMetaData(element));
  }

  private Tree convertElementToSlangAST(PsiElement element, TreeMetaData metaData) {
    if (isError(element)) {
      throw new ParseException("Cannot convert file due to syntactic errors", metaData.textRange().start());
    } else if (element instanceof KtBinaryExpression) {
      return createBinaryExpression(metaData, (KtBinaryExpression) element);
    } else if (element instanceof KtNameReferenceExpression) {
      return createIdentifierTree(metaData, element.getText());
    } else if (element instanceof KtBlockExpression) {
      List<Tree> statementOrExpressions = list(((KtBlockExpression) element).getStatements().stream());
      return new BlockTreeImpl(metaData, statementOrExpressions);
    } else if (element instanceof KtFile) {
      return new TopLevelTreeImpl(metaData, list(Arrays.stream(element.getChildren())), metaDataProvider.allComments());
    } else if (element instanceof KtFunction) {
      return createFunctionDeclarationTree(metaData, (KtFunction) element);
    } else if (element instanceof KtIfExpression) {
      return createIfTree(metaData, (KtIfExpression) element);
    } else if (element instanceof KtWhenExpression) {
      return createMatchTree(metaData, (KtWhenExpression) element);
    } else if (element instanceof KtWhenEntry) {
      return createMatchCase(metaData, (KtWhenEntry) element);
    } else if (isLiteral(element)) {
      return new LiteralTreeImpl(metaData, element.getText());
    } else if (element instanceof KtOperationExpression) {
      return createOperationExpression(metaData, (KtOperationExpression) element);
    } else if (element instanceof KtParameter && ((KtParameter) element).getName() != null) {
      return createIdentifierTree(metaData, ((KtParameter) element).getName());
    } else {
      return new NativeTreeImpl(metaData, new KotlinNativeKind(element), list(Arrays.stream(element.getChildren())));
    }
  }

  private Tree createFunctionDeclarationTree(TreeMetaData metaData, KtFunction functionElement) {
    List<Tree> modifiers = getModifierList(functionElement.getModifierList());
    PsiElement nameIdentifier = functionElement.getNameIdentifier();
    Tree returnType = null;
    IdentifierTree identifierTree = null;
    List<Tree> parametersList = list(functionElement.getValueParameters().stream());
    Tree bodyTree = createElement(functionElement.getBodyExpression());
    KtTypeElement typeElement = functionElement.getTypeReference() != null ? functionElement.getTypeReference().getTypeElement() : null;
    String name = functionElement.getName();

    if (typeElement != null) {
      returnType = new IdentifierTreeImpl(getTreeMetaData(typeElement), typeElement.getText());
    }
    if (nameIdentifier != null && name != null) {
      identifierTree = new IdentifierTreeImpl(getTreeMetaData(nameIdentifier), name);
    }
    if (bodyTree != null) {
      // FIXME are we sure we want body of function as block tree ?
      if (!(bodyTree instanceof BlockTree)) {
        bodyTree = new BlockTreeImpl(bodyTree.metaData(), Collections.singletonList(bodyTree));
      }

      // Set bodyTree to null for empty lambda functions
      if (bodyTree.children().isEmpty()) {
        bodyTree = null;
      }
    }

    return new FunctionDeclarationTreeImpl(metaData, modifiers, returnType, identifierTree, parametersList, (BlockTree) bodyTree);
  }

  private List<Tree> getModifierList(@Nullable KtModifierList modifierList) {
    if (modifierList == null) {
      return Collections.emptyList();
    }
    return Arrays.stream(KtTokens.MODIFIER_KEYWORDS_ARRAY)
      .map(modifierList::getModifier)
      .filter(Objects::nonNull)
      .map(element -> {
        NativeKind modifierKind = new KotlinNativeKind(element, element.getText());
        return new NativeTreeImpl(getTreeMetaData(element), modifierKind, Collections.emptyList());
      })
      .collect(Collectors.toList());
  }

  private Tree createIfTree(TreeMetaData metaData, KtIfExpression element) {
    Tree condition = createElement(element.getCondition());
    Tree thenBranch = createElement(element.getThen());
    Tree elseBranch = createElement(element.getElse());
    if (condition == null) {
      throw new ParseException("Invalid 'If' structure. 'condition' cannot be null", metaData.textRange().start());
    }

    if (thenBranch == null) {
      // Kotlin allows for a null then branch, which we match to a native since this is not allowed in Slang
      thenBranch = nullNative(condition.metaData());
    }
    return new IfTreeImpl(metaData, condition, thenBranch, elseBranch);
  }

  private static Tree createIdentifierTree(TreeMetaData metaData, @Nullable String name) {
    if (name == null) {
      throw new ParseException("Identifier name cannot be null", metaData.textRange().start());
    }
    return new IdentifierTreeImpl(metaData, name);
  }

  private static Tree nullNative(TreeMetaData metaData) {
    // We use the metadata of the parent here, as a NULL kind has no range itself
    return new NativeTreeImpl(metaData, NULL_NATIVE_KIND, Collections.emptyList());
  }

  private Tree createMatchTree(TreeMetaData metaData, KtWhenExpression element) {
    Tree subjectExpression = createElement(element.getSubjectExpression());
    if (subjectExpression == null) {
      subjectExpression = nullNative(metaData);
    }
    List<MatchCaseTree> whenExpressions = list(element.getEntries().stream()).stream()
      .map(MatchCaseTree.class::cast)
      .collect(Collectors.toList());
    return new MatchTreeImpl(metaData, subjectExpression, whenExpressions);
  }

  private Tree createMatchCase(TreeMetaData metaData, KtWhenEntry element) {
    Tree body = createElement(element.getExpression());
    if (body == null) {
      throw new ParseException("invalid 'when' structure. 'condition' in 'when' entries cannot be null", metaData.textRange().start());
    }
    Tree conditionExpression = null;
    if (!element.isElse()) {
      List<Tree> conditionsList = list(Arrays.stream(element.getConditions()));
      TextPointer startPointer = conditionsList.get(0).metaData().textRange().start();
      TextPointer endPointer = conditionsList.get(conditionsList.size() - 1).metaData().textRange().end();
      TextRange textRange = new TextRangeImpl(startPointer, endPointer);
      TreeMetaData treeMetaData = metaDataProvider.metaData(textRange);
      conditionExpression = new NativeTreeImpl(treeMetaData, new KotlinNativeKind(KtWhenCondition.class), conditionsList);
    }
    return new MatchCaseTreeImpl(metaData, conditionExpression, body);
  }

  private Tree createBinaryExpression(TreeMetaData metaData, KtBinaryExpression element) {
    Tree leftOperand = createElement(element.getLeft());
    Tree rightOperand = createElement(element.getRight());
    if (leftOperand == null) {
      leftOperand = nullNative(metaData);
    }
    if (rightOperand == null) {
      rightOperand = nullNative(metaData);
    }
    KtToken operationToken = element.getOperationReference().getOperationSignTokenType();
    Operator operator = TOKENS_OPERATOR_MAP.get(operationToken);
    AssignmentExpressionTree.Operator assignmentOperator = ASSIGNMENTS_OPERATOR_MAP.get(operationToken);
    if (operator != null) {
      return new BinaryExpressionTreeImpl(metaData, operator, leftOperand, rightOperand);
    } else if (assignmentOperator != null) {
      return new AssignmentExpressionTreeImpl(metaData, assignmentOperator, leftOperand, rightOperand);
    } else {
      // FIXME ensure they are all supported. Ex: Add '/=' for assignments
      return createOperationExpression(metaData, element);
    }
  }

  private Tree createOperationExpression(TreeMetaData metaData, KtOperationExpression operationExpression) {
    KotlinNativeKind nativeKind = new KotlinNativeKind(operationExpression, operationExpression.getOperationReference().getReferencedNameElement().getText());
    return new NativeTreeImpl(metaData, nativeKind, list(Arrays.stream(operationExpression.getChildren())));
  }

  private TreeMetaData getTreeMetaData(PsiElement element) {
    return metaDataProvider.metaData(KotlinTextRanges.textRange(psiDocument, element));
  }

  private List<Tree> list(Stream<? extends PsiElement> stream) {
    // Filtering out null elements as they can appear in the AST in cases of comments or other leaf elements
    return stream
      .map(this::createElement)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  Tree getSLangAST() {
    return sLangAST;
  }

  private static boolean shouldSkipElement(PsiElement element) {
    return element instanceof PsiWhiteSpace || element instanceof LeafPsiElement;
  }

  private static boolean isError(PsiElement element) {
    return element instanceof PsiErrorElement;
  }

  private static boolean isLiteral(PsiElement element) {
    return element instanceof KtConstantExpression
      || element instanceof KtLiteralStringTemplateEntry
      || element instanceof KtEscapeStringTemplateEntry
      || (element instanceof KtStringTemplateExpression && !((KtStringTemplateExpression) element).hasInterpolation());
  }
}
