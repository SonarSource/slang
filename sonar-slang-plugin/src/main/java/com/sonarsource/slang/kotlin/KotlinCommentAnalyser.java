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

import com.sonarsource.slang.api.ASTConverter;
import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.analyzer.commons.TokenLocation;

public class KotlinCommentAnalyser implements ASTConverter {
  @Override
  public Tree parse(String content) {
    Tree ast;
    String wrappedContent = "fun { " + content + "}";
    try {
      ast = new KotlinConverter().parse(wrappedContent);
      BlockTree blockTree = (BlockTree) ast.children().get(0).children().get(0);
      if (isNotCompletelyParsed(wrappedContent, ast.metaData().textRange()) || isSimpleExpression(blockTree)) {
        return null;
      }
    } catch (Exception e) {
      ast = null;
    }
    return ast;
  }

  private static boolean isNotCompletelyParsed(String content, TextRange textRange) {
    TextPointer start = textRange.start();
    TextPointer end = textRange.end();
    TokenLocation tokenLocation = new TokenLocation(start.line(), start.lineOffset(), content);
    return end.line() != tokenLocation.endLine() || end.lineOffset() != tokenLocation.endLineOffset();
  }


  private static boolean isSimpleExpression(BlockTree tree) {
    return tree.descendants()
      .allMatch(element -> element instanceof IdentifierTree || element instanceof NativeTree);
  }

}
