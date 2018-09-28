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

import org.sonarsource.slang
import org.sonarsource.slang.api.{NativeTree, TextRange, Token, TreeMetaData}
import org.sonarsource.slang.impl._
import org.sonarsource.slang.visitors.TreePrinter

import scala.collection.JavaConverters._
import scala.meta._
import scala.meta.internal.tokenizers.keywords
import scala.meta.tokens.Token.Comment

class ScalaConverter extends slang.api.ASTConverter {

  def parse(code: String): slang.api.Tree = {
    val metaTree: scala.meta.Tree = code.parse[Source] match {
      case scala.meta.parsers.Parsed.Success(tree) => tree
      case scala.meta.parsers.Parsed.Error(pos, _, _) =>
        throw new slang.api.ParseException("Unable to parse file content.", textRange(pos).start())
    }

    val allTokens = metaTree.tokens
      .filter(t => !t.is[Comment])
      .filter(t => t.text.trim.nonEmpty)
      .map(t => new TokenImpl(textRange(t.pos), t.text, tokenType(t)))
      .asInstanceOf[IndexedSeq[Token]]
      .asJava

    val allComments = metaTree.tokens
      .filter(t => t.is[Comment])
      .map(t => createComment(t))
      .asJava

    val metaDataProvider = new TreeMetaDataProvider(allComments, allTokens)
    new TreeConversion(metaDataProvider).convert(metaTree)
  }

  private class TreeConversion(metaDataProvider: TreeMetaDataProvider) {

    def convert(metaTree: scala.meta.Tree): slang.api.Tree = {
      val metaData = metaDataProvider.metaData(textRange(metaTree))
      metaTree match {
        case scala.meta.Source(stats) =>
          createTopLevelTree(metaData, stats)
        case scala.meta.Pkg(ref, stats) =>
          new PackageDeclarationTreeImpl(metaData, convert(ref :: stats))
        case scala.meta.Import(importers) =>
          new ImportDeclarationTreeImpl(metaData, convert(importers))
        case lit: scala.meta.Lit.String =>
          new StringLiteralTreeImpl(metaData, "\"" + lit.value + "\"")
        case lit: scala.meta.Lit.Int =>
          new IntegerLiteralTreeImpl(metaData, lit.syntax)
        case lit: scala.meta.Lit =>
          new LiteralTreeImpl(metaData, lit.syntax)
        case _ =>
          val nativeKind = ScalaNativeKind(metaTree.getClass)
          new NativeTreeImpl(metaData, nativeKind, convert(metaTree.children))
      }
    }

    private def createTopLevelTree(metaData: TreeMetaData, stats: List[Stat]) = {
      val convertedStats = convert(stats)
      val firstCpdToken = convertedStats.stream()
        // The first child of a Package is the "ref" of the package
        .flatMap(t => if (t.isInstanceOf[slang.api.PackageDeclarationTree]) t.children().stream.skip(1) else List(t).asJava.stream)
        .filter(t => !t.isInstanceOf[slang.api.ImportDeclarationTree])
        .map[slang.api.Token](t => t.metaData.tokens.get(0))
        .findFirst()
        .orElse(null)
      new TopLevelTreeImpl(metaData, convertedStats, metaDataProvider.allComments, firstCpdToken)
    }

    private def convert(trees: scala.List[scala.meta.Tree]): java.util.List[slang.api.Tree] = {
      trees.filter(t => t.pos.start != t.pos.end)
        .map(t => convert(t))
        .asJava
    }

  }

  case class ScalaNativeKind(treeClass: Class[_ <: scala.meta.Tree]) extends slang.api.NativeKind {
  }

  def tokenType(token: scala.meta.tokens.Token): Token.Type = {
    if (token.is[scala.meta.tokens.Token.Constant.String]) {
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
    val start = range.start
    val end = range.end
    val contentRange = TextRanges.range(start.line, start.lineOffset + 2, end.line, end.lineOffset - suffixLength)
    new CommentImpl(t.text, contentText, range, contentRange)
  }

  def textRange(tree: scala.meta.Tree): TextRange = {
    textRange(tree.pos)
  }

  def textRange(pos: scala.meta.Position): TextRange = {
    TextRanges.range(pos.startLine + 1, pos.startColumn, pos.endLine + 1, pos.endColumn)
  }

}
