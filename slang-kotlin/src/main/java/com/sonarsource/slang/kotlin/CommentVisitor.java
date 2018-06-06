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

import com.sonarsource.slang.api.Comment;
import com.sonarsource.slang.impl.CommentImpl;
import com.sonarsource.slang.kotlin.utils.KotlinTextRanges;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;

import static org.jetbrains.kotlin.lexer.KtTokens.BLOCK_COMMENT;
import static org.jetbrains.kotlin.lexer.KtTokens.DOC_COMMENT;
import static org.jetbrains.kotlin.lexer.KtTokens.EOL_COMMENT;
import static org.jetbrains.kotlin.lexer.KtTokens.SHEBANG_COMMENT;

public class CommentVisitor extends KtTreeVisitorVoid {

  private final Document psiDocument;
  private final List<Comment> allComments;

  public CommentVisitor(Document psiDocument) {
    this.psiDocument = psiDocument;
    this.allComments = new LinkedList<>();
  }

  public void visitElement(@NotNull PsiElement element) {
    if (element instanceof PsiComment) {
      allComments.add(createComment((PsiComment) element));
    }
    super.visitElement(element);
  }

  private Comment createComment(@NotNull PsiComment element) {
    String textWithDelimiters = element.getText();
    return new CommentImpl(
      commentContent(element, textWithDelimiters),
      textWithDelimiters,
      KotlinTextRanges.textRange(psiDocument, element));
  }

  private static String commentContent(@NotNull PsiComment element, @NotNull String textWithDelimiters) {
    IElementType tokenType = element.getTokenType();
    int length = textWithDelimiters.length();

    if (BLOCK_COMMENT.equals(tokenType)) {
      return textWithDelimiters.substring(2, length - 2);
    } else if (DOC_COMMENT.equals(tokenType)) {
      return textWithDelimiters.substring(3, length - 2);
    } else if (EOL_COMMENT.equals(tokenType) || SHEBANG_COMMENT.equals(tokenType)) {
      return textWithDelimiters.substring(2);
    } else {
      // FIXME error message: unknown comment type
      return textWithDelimiters;
    }
  }

  public List<Comment> getAllComments() {
    return allComments;
  }
}
