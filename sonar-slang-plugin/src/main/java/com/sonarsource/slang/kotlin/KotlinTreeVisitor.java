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
import com.sonarsource.slang.api.CatchTree;
import com.sonarsource.slang.api.ExceptionHandlingTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.NativeKind;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import com.sonarsource.slang.api.UnaryExpressionTree;
import com.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.BlockTreeImpl;
import com.sonarsource.slang.impl.CatchTreeImpl;
import com.sonarsource.slang.impl.ClassDeclarationTreeImpl;
import com.sonarsource.slang.impl.ExceptionHandlingTreeImpl;
import com.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import com.sonarsource.slang.impl.IdentifierTreeImpl;
import com.sonarsource.slang.impl.IfTreeImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.LoopTreeImpl;
import com.sonarsource.slang.impl.MatchCaseTreeImpl;
import com.sonarsource.slang.impl.MatchTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import com.sonarsource.slang.impl.ParameterTreeImpl;
import com.sonarsource.slang.impl.StringLiteralTreeImpl;
import com.sonarsource.slang.impl.TextRangeImpl;
import com.sonarsource.slang.impl.TokenImpl;
import com.sonarsource.slang.impl.TopLevelTreeImpl;
import com.sonarsource.slang.impl.TreeMetaDataProvider;
import com.sonarsource.slang.impl.UnaryExpressionTreeImpl;
import com.sonarsource.slang.impl.VariableDeclarationTreeImpl;
import com.sonarsource.slang.kotlin.utils.KotlinTextRanges;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
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
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtCatchClause;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtConstructor;
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry;
import org.jetbrains.kotlin.psi.KtDoWhileExpression;
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFinallySection;
import org.jetbrains.kotlin.psi.KtForExpression;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtLoopExpression;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtOperationExpression;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtTryExpression;
import org.jetbrains.kotlin.psi.KtTypeElement;
import org.jetbrains.kotlin.psi.KtTypeParameterList;
import org.jetbrains.kotlin.psi.KtUnaryExpression;
import org.jetbrains.kotlin.psi.KtWhenCondition;
import org.jetbrains.kotlin.psi.KtWhenEntry;
import org.jetbrains.kotlin.psi.KtWhenExpression;
import org.jetbrains.kotlin.psi.KtWhileExpression;

import static com.sonarsource.slang.api.LoopTree.LoopKind.DOWHILE;
import static com.sonarsource.slang.api.LoopTree.LoopKind.FOR;
import static com.sonarsource.slang.api.LoopTree.LoopKind.WHILE;

class KotlinTreeVisitor {
  private static final Map<KtToken, Operator> BINARY_OPERATOR_MAP = Collections.unmodifiableMap(Stream.of(
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

  private static final Map<KtToken, UnaryExpressionTree.Operator> UNARY_OPERATOR_MAP = Collections.unmodifiableMap(Stream.of(
    new SimpleEntry<>(KtTokens.EXCL, UnaryExpressionTree.Operator.NEGATE))
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
  private final Deque<Boolean> enumClassDeclaration;

  public KotlinTreeVisitor(PsiFile psiFile, TreeMetaDataProvider metaDataProvider) {
    this.enumClassDeclaration = new ArrayDeque<>();
    this.psiDocument = psiFile.getViewProvider().getDocument();
    this.metaDataProvider = metaDataProvider;
    this.sLangAST = createMandatoryElement(psiFile);
  }

  private Tree createMandatoryElement(@Nullable PsiElement psiElement) {
    Tree element = createElement(psiElement);
    if (element == null) {
      TextPointer errorLocation = psiElement != null ? getTreeMetaData(psiElement).textRange().start() : null;
      throw new ParseException("A mandatory AST element is missing from the grammar", errorLocation);
    }
    return element;
  }

  @CheckForNull
  private Tree createElement(@Nullable PsiElement element) {
    if (element == null || shouldSkipElement(element)) {
      // skip tokens and whitespaces nodes in kotlin AST
      return null;
    }
    TreeMetaData metaData = getTreeMetaData(element);
    TextRange textRange = metaData.textRange();
    if (textRange.start().equals(textRange.end())) {
      return null;
    }
    return convertElementToSlangAST(element, metaData);
  }

  private Tree convertElementToSlangAST(PsiElement element, TreeMetaData metaData) {
    if (isError(element)) {
      throw new ParseException("Cannot convert file due to syntactic errors", metaData.textRange().start());
    } else if (element instanceof KtOperationExpression) {
      return createOperationExpression(metaData, (KtOperationExpression) element);
    } else if (element instanceof KtNameReferenceExpression) {
      return createIdentifierTree(metaData, element.getText());
    } else if (element instanceof KtBlockExpression) {
      List<Tree> statementOrExpressions = list(((KtBlockExpression) element).getStatements().stream());
      return new BlockTreeImpl(metaData, statementOrExpressions);
    } else if (element instanceof KtFile) {
      return new TopLevelTreeImpl(metaData, list(Arrays.stream(element.getChildren())), metaDataProvider.allComments());
    } else if (element instanceof KtClass) {
      return createClassDeclarationTree(metaData, (KtClass) element);
    } else if (element instanceof KtFunction) {
      return createFunctionDeclarationTree(metaData, (KtFunction) element);
    } else if (element instanceof KtIfExpression) {
      return createIfTree(metaData, (KtIfExpression) element);
    } else if (element instanceof KtWhenExpression) {
      return createMatchTree(metaData, (KtWhenExpression) element);
    } else if (element instanceof KtWhenEntry) {
      return createMatchCase(metaData, (KtWhenEntry) element);
    } else if (element instanceof KtLoopExpression) {
      return createLoopTree(metaData, (KtLoopExpression) element);
    } else if (element instanceof KtTryExpression) {
      return createExceptionHandling(metaData, (KtTryExpression) element);
    } else if (element instanceof KtCatchClause) {
      return createCatchTree(metaData, (KtCatchClause) element);
    } else if (element instanceof KtFinallySection) {
      return createElement(((KtFinallySection) element).getFinalExpression());
    } else if (isLiteral(element)) {
      return createLiteral(metaData, element);
    } else if (element instanceof KtParameter) {
      return createParameter(metaData, (KtParameter) element);
    } else if (element instanceof KtProperty) {
      return createVariableDeclaration(metaData, (KtProperty) element);
    } else {
      return convertElementToNative(element, metaData);
    }
  }

  private Tree createClassDeclarationTree(TreeMetaData metaData, KtClass ktClass) {
    PsiElement nameIdentifier = ktClass.getNameIdentifier();
    IdentifierTree identifier = null;
    Boolean isInsideEnum = enumClassDeclaration.peekLast();
    enumClassDeclaration.addLast(ktClass.isEnum());
    List<Tree> children = list(Arrays.stream(ktClass.getChildren()));
    enumClassDeclaration.removeLast();

    if (nameIdentifier != null) {
      identifier = createIdentifierTree(getTreeMetaData(nameIdentifier), nameIdentifier.getText());
      children.add(identifier);
    }

    Tree nativeClassDecl = createNativeTree(metaData, new KotlinNativeKind(ktClass), children);
    Tree classDecl = nativeClassDecl;
    if (isInsideEnum == null || !isInsideEnum) {
      classDecl = new ClassDeclarationTreeImpl(metaData, identifier, nativeClassDecl);
    }
    return classDecl;
  }

  private Tree convertElementToNative(PsiElement element, TreeMetaData metaData) {
    if (element instanceof KtDestructuringDeclarationEntry || isSimpleStringLiteralEntry(element)) {
      // To differentiate between the native trees of complex string template entries, we add the string value to the native kind
      return createNativeTree(metaData, new KotlinNativeKind(element, element.getText()), element);
    } else {
      return createNativeTree(metaData, new KotlinNativeKind(element), element);
    }
  }

  private Tree createFunctionDeclarationTree(TreeMetaData metaData, KtFunction functionElement) {
    if (functionElement instanceof KtConstructor || functionElement.getReceiverTypeReference() != null) {
      // Constructors and extension functions: for now they are considered as native elements instead of function declaration to avoid FP
      return createNativeTree(metaData, new KotlinNativeKind(functionElement), functionElement);
    }

    List<Tree> modifiers = getModifierList(functionElement.getModifierList());
    PsiElement nameIdentifier = functionElement.getNameIdentifier();
    Tree returnType = null;
    IdentifierTree identifierTree = null;
    List<Tree> parametersList = list(functionElement.getValueParameters().stream());

    Tree bodyTree = createElement(functionElement.getBodyExpression());
    KtTypeElement typeElement = functionElement.getTypeReference() != null ? functionElement.getTypeReference().getTypeElement() : null;
    String name = functionElement.getName();
    KtTypeParameterList typeParameterList = functionElement.getTypeParameterList();

    if (typeElement != null) {
      returnType = new IdentifierTreeImpl(getTreeMetaData(typeElement), typeElement.getText());
    }
    if (nameIdentifier != null && name != null) {
      identifierTree = new IdentifierTreeImpl(getTreeMetaData(nameIdentifier), name);
    }
    if (bodyTree != null && !(bodyTree instanceof BlockTree)) {
      // FIXME are we sure we want body of function as block tree ?
      bodyTree = new BlockTreeImpl(bodyTree.metaData(), Collections.singletonList(bodyTree));
    }
    List<Tree> typeParameters;
    if (typeParameterList != null) {
      typeParameters = list(Arrays.stream(typeParameterList.getChildren()));
    } else {
      typeParameters = Collections.emptyList();
    }

    return new FunctionDeclarationTreeImpl(metaData, modifiers, returnType, identifierTree, parametersList, (BlockTree) bodyTree, typeParameters);
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
        return createNativeTree(getTreeMetaData(element), modifierKind, Collections.emptyList());
      })
      .collect(Collectors.toList());
  }

  private Tree createIfTree(TreeMetaData metaData, KtIfExpression element) {
    Tree condition = createMandatoryElement(element.getCondition());
    Tree thenBranch = createElement(element.getThen());
    Tree elseBranch = createElement(element.getElse());
    Token ifToken = toSlangToken(element.getIfKeyword());
    Token elseToken = element.getElseKeyword() != null ? toSlangToken(element.getElseKeyword()) : null;
    if (thenBranch == null) {
      // Kotlin allows for a null then branch, which we match to a native since this is not allowed in Slang
      List<Tree> children = elseBranch != null ? Arrays.asList(condition, elseBranch) : Collections.singletonList(condition);
      return createNativeTree(metaData, new KotlinNativeKind(element), children);
    }
    return new IfTreeImpl(metaData, condition, thenBranch, elseBranch, ifToken, elseToken);
  }

  private Tree createParameter(TreeMetaData metaData, KtParameter ktParameter) {
    Tree type = createElement(ktParameter.getTypeReference());
    PsiElement nameIdentifier = ktParameter.getNameIdentifier();

    // For some reason the Identifier is not among the Parameter children array, so for now we add this information to the native kind
    if (nameIdentifier == null) {
      return createNativeTree(metaData, new KotlinNativeKind(ktParameter), ktParameter);
    }

    IdentifierTree identifier = createIdentifierTree(getTreeMetaData(nameIdentifier), nameIdentifier.getText());
    Tree initializer = createElement(ktParameter.getDefaultValue());
    ParameterTreeImpl parameterTree = new ParameterTreeImpl(metaData, identifier, type);
    if (initializer != null) {
      return createNativeTree(metaData, new KotlinNativeKind(ktParameter), Arrays.asList(parameterTree, initializer));
    }
    return parameterTree;
  }

  private Tree createVariableDeclaration(TreeMetaData metaData, KtProperty ktProperty) {
    PsiElement nameIdentifier = ktProperty.getNameIdentifier();

    if (nameIdentifier == null) {
      return convertElementToNative(ktProperty, metaData);
    } else if (!ktProperty.isLocal() || ktProperty.hasDelegate()) {
      return createNativeTree(metaData, new KotlinNativeKind(ktProperty, nameIdentifier.getText()), ktProperty);
    }

    IdentifierTree identifierTree = new IdentifierTreeImpl(getTreeMetaData(nameIdentifier), nameIdentifier.getText());
    Tree typeTree = createElement(ktProperty.getTypeReference());
    Tree initializerTree = createElement(ktProperty.getInitializer());
    boolean isVal = !ktProperty.isVar();
    return new VariableDeclarationTreeImpl(metaData, identifierTree, typeTree, initializerTree, isVal);
  }

  private Tree createNativeTree(TreeMetaData metaData, NativeKind kind, PsiElement element) {
    return createNativeTree(metaData, kind, list(Arrays.stream(element.getChildren())));
  }

  private static Tree createNativeTree(TreeMetaData metaData, NativeKind kind, List<Tree> children) {
    return new NativeTreeImpl(metaData, kind, children);
  }

  private static IdentifierTree createIdentifierTree(TreeMetaData metaData, String name) {
    return new IdentifierTreeImpl(metaData, name);
  }

  private Tree createMatchTree(TreeMetaData metaData, KtWhenExpression element) {
    Tree subjectExpression = createElement(element.getSubjectExpression());
    List<Tree> whenExpressions = list(element.getEntries().stream());
    if (subjectExpression == null) {
      return createNativeTree(metaData, new KotlinNativeKind(element), whenExpressions);
    }
    return new MatchTreeImpl(metaData,
      subjectExpression,
      whenExpressions.stream()
        .map(MatchCaseTree.class::cast)
        .collect(Collectors.toList()),
      toSlangToken(element.getWhenKeyword()));
  }

  private Tree createMatchCase(TreeMetaData metaData, KtWhenEntry element) {
    Tree body = createMandatoryElement(element.getExpression());
    Tree conditionExpression = null;
    if (!element.isElse()) {
      List<Tree> conditionsList = list(Arrays.stream(element.getConditions()));
      TextPointer startPointer = conditionsList.get(0).metaData().textRange().start();
      TextPointer endPointer = conditionsList.get(conditionsList.size() - 1).metaData().textRange().end();
      TextRange textRange = new TextRangeImpl(startPointer, endPointer);
      TreeMetaData treeMetaData = metaDataProvider.metaData(textRange);
      conditionExpression = createNativeTree(treeMetaData, new KotlinNativeKind(KtWhenCondition.class), conditionsList);
    }
    return new MatchCaseTreeImpl(metaData, conditionExpression, body);
  }

  private Tree createLoopTree(TreeMetaData metaData, KtLoopExpression ktLoopExpression) {
    Tree body = createElement(ktLoopExpression.getBody());
    if (body == null) {
      return convertElementToNative(ktLoopExpression, metaData);
    }

    if (ktLoopExpression instanceof KtForExpression) {
      KtForExpression forExpression = (KtForExpression) ktLoopExpression;
      Tree parameter = createElement(forExpression.getLoopParameter());
      Tree range = createElement(forExpression.getLoopRange());
      if (parameter == null || range == null) {
        return convertElementToNative(ktLoopExpression, metaData);
      }
      TextPointer startPointer = parameter.textRange().start();
      TextPointer endPointer = range.textRange().end();
      TextRange textRange = new TextRangeImpl(startPointer, endPointer);
      TreeMetaData conditionMetaData = metaDataProvider.metaData(textRange);
      Tree condition = createNativeTree(conditionMetaData, new KotlinNativeKind(ktLoopExpression), Arrays.asList(parameter, range));
      Token forToken = toSlangToken(forExpression.getForKeyword());
      return new LoopTreeImpl(metaData, condition, body, FOR, forToken);
    } else if (ktLoopExpression instanceof KtWhileExpression) {
      KtWhileExpression whileExpression = (KtWhileExpression) ktLoopExpression;
      Tree condition = createElement(whileExpression.getCondition());
      if (condition == null) {
        return convertElementToNative(ktLoopExpression, metaData);
      }
      Token whileToken = toSlangToken(whileExpression.getFirstChild());
      return new LoopTreeImpl(metaData, condition, body, WHILE, whileToken);
    } else {
      KtDoWhileExpression doWhileExpression = (KtDoWhileExpression) ktLoopExpression;
      Tree condition = createElement(doWhileExpression.getCondition());
      if (condition == null) {
        return convertElementToNative(ktLoopExpression, metaData);
      }
      Token whileToken = toSlangToken(doWhileExpression.getFirstChild());
      return new LoopTreeImpl(metaData, condition, body, DOWHILE, whileToken);
    }
  }

  private Tree createOperationExpression(TreeMetaData metaData, KtOperationExpression operationExpression) {
    if (operationExpression instanceof KtBinaryExpression) {
      return createBinaryExpression(metaData, (KtBinaryExpression) operationExpression);
    } else if (operationExpression instanceof KtUnaryExpression) {
      return createUnaryExpression(metaData, (KtUnaryExpression) operationExpression);
    }

    return createNativeOperationExpression(metaData, operationExpression);
  }

  private ExceptionHandlingTree createExceptionHandling(TreeMetaData metadata, KtTryExpression element) {
    Tree tryTree = createMandatoryElement(element.getTryBlock());
    List<Tree> catchTreeList = list(element.getCatchClauses().stream());
    List<CatchTree> catchTrees = catchTreeList.stream().map(CatchTree.class::cast).collect(Collectors.toList());
    Tree finallyTree = createElement(element.getFinallyBlock());
    return new ExceptionHandlingTreeImpl(metadata, tryTree, catchTrees, finallyTree);
  }

  private CatchTree createCatchTree(TreeMetaData metaData, KtCatchClause element) {
    Tree catchBody = createMandatoryElement(element.getCatchBody());
    if (element.getCatchParameter() == null) {
      return new CatchTreeImpl(metaData, null, catchBody);
    } else {
      return new CatchTreeImpl(metaData, createParameter(metaData, element.getCatchParameter()), catchBody);
    }
  }

  private Tree createBinaryExpression(TreeMetaData metaData, KtBinaryExpression element) {
    Tree leftOperand = createElement(element.getLeft());
    Tree rightOperand = createElement(element.getRight());
    KtToken operationToken = element.getOperationReference().getOperationSignTokenType();
    if (leftOperand == null || rightOperand == null) {
      // Binary expression with a single or no operand, which cannot exist in Slang AST
      List<Tree> children = Stream.of(leftOperand, rightOperand)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
      return createNativeTree(metaData, new KotlinNativeKind(element, operationToken), children);
    }

    Operator operator = BINARY_OPERATOR_MAP.get(operationToken);
    AssignmentExpressionTree.Operator assignmentOperator = ASSIGNMENTS_OPERATOR_MAP.get(operationToken);
    if (operator != null) {
      return new BinaryExpressionTreeImpl(metaData, operator, leftOperand, rightOperand);
    } else if (assignmentOperator != null) {
      return new AssignmentExpressionTreeImpl(metaData, assignmentOperator, leftOperand, rightOperand);
    } else {
      // FIXME ensure they are all supported. Ex: Add '/=' for assignments
      return createNativeOperationExpression(metaData, element);
    }
  }

  private Tree createUnaryExpression(TreeMetaData metaData, KtUnaryExpression element) {
    Tree operand = createElement(element.getBaseExpression());
    IElementType operationToken = element.getOperationToken();
    UnaryExpressionTree.Operator operator = UNARY_OPERATOR_MAP.get(operationToken);

    if (operand == null || operator == null) {
      return createNativeOperationExpression(metaData, element);
    }
    return new UnaryExpressionTreeImpl(metaData, operator, operand);
  }

  private static Tree createLiteral(TreeMetaData metaData, PsiElement element) {
    if (isSimpleStringLiteral(element)) {
      return new StringLiteralTreeImpl(metaData, element.getText());
    }
    return new LiteralTreeImpl(metaData, element.getText());
  }

  private Tree createNativeOperationExpression(TreeMetaData metaData, KtOperationExpression operationExpression) {
    NativeKind nativeKind = new KotlinNativeKind(operationExpression, operationExpression.getOperationReference().getReferencedNameElement().getText());
    return createNativeTree(metaData, nativeKind, operationExpression);
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
      || isSimpleStringLiteral(element);
  }

  private static boolean isSimpleStringLiteral(PsiElement element) {
    return element instanceof KtStringTemplateExpression && !((KtStringTemplateExpression) element).hasInterpolation();
  }

  private static boolean isSimpleStringLiteralEntry(PsiElement element) {
    return element instanceof KtLiteralStringTemplateEntry || element instanceof KtEscapeStringTemplateEntry;
  }

  private TokenImpl toSlangToken(PsiElement psiElement) {
    return new TokenImpl(
      KotlinTextRanges.textRange(psiDocument, psiElement),
      psiElement.getText(),
      Token.Type.KEYWORD);
  }

}
