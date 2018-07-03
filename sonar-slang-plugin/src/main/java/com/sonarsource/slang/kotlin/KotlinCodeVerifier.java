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

import com.sonarsource.slang.api.CodeVerifier;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS;
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtIsExpression;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression;
import org.jetbrains.kotlin.psi.KtPrefixExpression;
import org.jetbrains.kotlin.psi.KtThisExpression;

public class KotlinCodeVerifier implements CodeVerifier {
  private static final List<String> KDOC_TAGS = Arrays.asList(
    "@param", "@name", "@return", "@constructor", "@receiver", "@property", "@throws",
    "@exception", "@sample", "@see", "@author", "@since", "@suppress");

  @Override
  public boolean containsCode(String content) {
    String wrappedContent = "fun function () { " + content + " }";
    if (isKDoc(content)) {
      return false;
    }
    try {
      KotlinConverter.KotlinTree kotlinTree = new KotlinConverter.KotlinTree(wrappedContent);
      return !isSimpleExpression(kotlinTree.psiFile);
    } catch (ParseException e) {
      // do nothing
    }
    return false;
  }

  private static boolean isKDoc(String content) {
    return KDOC_TAGS.stream().anyMatch(content::contains);
  }

  private static boolean isSimpleExpression(PsiFile tree) {
    PsiElement content = tree.getLastChild().getLastChild();
    PsiElement[] elements = content.getChildren();
    return Arrays.stream(elements).allMatch(element ->
      element instanceof KtNameReferenceExpression ||
        element instanceof KtCollectionLiteralExpression ||
        element instanceof KtConstantExpression ||
        isInfixExpression(element)) || isSingleExpression(elements);
  }

  private static boolean isSingleExpression(PsiElement [] elements) {
    if (elements.length != 1) {
      return false;
    }
    PsiElement element = elements[0];
    return element instanceof KtIsExpression ||
      element instanceof KtThisExpression ||
      element instanceof KtPrefixExpression ||
      element instanceof KtBinaryExpression ||
      element instanceof KtBinaryExpressionWithTypeRHS ||
      element instanceof KtDotQualifiedExpression;
  }

  private static boolean isInfixExpression(PsiElement element) {
    if (element instanceof KtBinaryExpression) {
      PsiElement[] binaryExprChildren = element.getChildren();
      return binaryExprChildren.length == 3 &&
        binaryExprChildren[1] instanceof KtOperationReferenceExpression;
    }
    return false;
  }

}
