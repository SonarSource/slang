package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.NativeKind;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import com.sonarsource.slang.impl.IdentifierImpl;
import com.sonarsource.slang.impl.LiteralTreeImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;

class KotlinTreeVisitor extends KtTreeVisitorVoid {
  private static final NativeKind GENERIC_NATIVE_KIND = new NativeKind() {
  };

  private List<Tree> currentChildren = new ArrayList<>();

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

  private static Tree createElement(PsiElement element, List<Tree> children) {
    TextRange textRange = null;
    if (element instanceof PsiWhiteSpace || element instanceof LeafPsiElement) {
      // skip tokens and whitespaces nodes in kotlin AST
      return null;
    } else if (element instanceof KtBinaryExpression) {
      BinaryExpressionTree.Operator operator = mapBinaryExpression(((KtBinaryExpression) element).getOperationToken());
      // child at index 1 is the KtOperationReferenceExpression
      return new BinaryExpressionTreeImpl(textRange, operator, children.get(0), children.get(2));
    } else if (element instanceof KtNameReferenceExpression) {
      return new IdentifierImpl(textRange, element.getText());
    } else if (element instanceof KtConstantExpression) {
      return new LiteralTreeImpl(textRange, element.getText());
    } else {
      return new NativeTreeImpl(textRange, GENERIC_NATIVE_KIND, children);
    }
  }

  private static BinaryExpressionTree.Operator mapBinaryExpression(IElementType operationTokenType) {
    if (KtTokens.EQEQ.equals(operationTokenType)) {
      return BinaryExpressionTree.Operator.EQUAL_TO;
    } else if (KtTokens.EXCLEQ.equals(operationTokenType)) {
      return BinaryExpressionTree.Operator.NOT_EQUAL_TO;
    } else if (KtTokens.LT.equals(operationTokenType)) {
      return BinaryExpressionTree.Operator.LESS_THAN;
    } else if (KtTokens.GT.equals(operationTokenType)) {
      return BinaryExpressionTree.Operator.GREATER_THAN;
    } else if (KtTokens.LTEQ.equals(operationTokenType)) {
      return BinaryExpressionTree.Operator.LESS_THAN_OR_EQUAL_TO;
    } else if (KtTokens.GTEQ.equals(operationTokenType)) {
      return BinaryExpressionTree.Operator.GREATER_THAN_OR_EQUAL_TO;
    } else if (KtTokens.OROR.equals(operationTokenType)) {
      return BinaryExpressionTree.Operator.CONDITIONAL_OR;
    } else if (KtTokens.ANDAND.equals(operationTokenType)) {
      return BinaryExpressionTree.Operator.CONDITIONAL_AND;
    }

    throw new IllegalStateException("Binary operation type not supported: " + operationTokenType);
  }

  Tree getSLangAST() {
    return currentChildren.get(0);
  }
}
