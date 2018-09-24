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
package org.sonarsource.scala.converter

import java.util.Collections

import org.sonarsource.slang
import org.sonarsource.slang.api.TextRange
import org.sonarsource.slang.api.Token
import org.sonarsource.slang.impl._

import scala.collection.JavaConverters._
import scala.meta._
import scala.meta.internal.tokenizers.keywords
import scala.meta.tokens.Token.Comment

class ScalaConverter extends slang.api.ASTConverter {

  def parse(code: String): slang.api.Tree = {
    val ast = code.parse[Source] match {
      case scala.meta.parsers.Parsed.Success(tree) => tree
      case scala.meta.parsers.Parsed.Error(pos, _, _) =>
        throw new slang.api.ParseException("Unable to parse file content.", textRange(pos).start())
    }

    val allTokens = ast.tokens
      .filter(t => !t.isInstanceOf[Comment])
      .filter(t => !t.text.trim.isEmpty)
      .map(t => new TokenImpl(textRange(t.pos), t.text, tokenType(t)))
      .asInstanceOf[IndexedSeq[Token]]
      .asJava

    val allComments = ast.tokens
      .filter(t => t.isInstanceOf[Comment])
      .map(t => createComment(t))
      .asJava

    val metaDataProvider = new TreeMetaDataProvider(allComments, allTokens)
    new TopLevelTreeImpl(metaDataProvider.metaData(textRange(ast)), Collections.emptyList(), allComments)
  }

  def tokenType(token: scala.meta.tokens.Token): Token.Type = {
    if (token.text.startsWith("\"")) {
      return Token.Type.STRING_LITERAL
    }
    if (keywords.contains(token.text)) {
      return Token.Type.KEYWORD
    }
    Token.Type.OTHER
  }

  private def createComment(t: scala.meta.tokens.Token): slang.api.Comment = {
    var suffixLength = 0
    if (t.text.startsWith("/*")) {
      suffixLength = 2
    }
    val contentText = t.text.substring(2, t.text.length - suffixLength)
    val range = textRange(t.pos)
    val start = range.start()
    val end = range.end()
    val contentRange = TextRanges.range(start.line(), start.lineOffset() + 2, end.line(), end.lineOffset() - suffixLength)
    new CommentImpl(t.text, contentText, range, contentRange)
  }

  def textRange(tree: scala.meta.Tree): TextRange = {
    textRange(tree.pos)
  }

  def textRange(pos: scala.meta.Position): TextRange = {
    TextRanges.range(pos.startLine + 1, pos.startColumn, pos.endLine + 1, pos.endColumn)
  }

}
