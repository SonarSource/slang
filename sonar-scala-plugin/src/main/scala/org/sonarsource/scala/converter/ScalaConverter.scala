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

import java.util.Collections.{emptyList, singletonList}

import org.sonarsource.slang
import org.sonarsource.slang.api
import org.sonarsource.slang.api.{NativeTree, TextRange, Token, TreeMetaData, IdentifierTree}
import org.sonarsource.slang.impl._

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
      if (metaTree.pos.start == metaTree.pos.end && metaTree.isNot[scala.meta.Source]) {
        return null
      }
      val metaData = treeMetaData(metaTree)
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
        case Term.Name(value) =>
          new IdentifierTreeImpl(metaData, value)
        case Type.Name(value) =>
          new IdentifierTreeImpl(metaData, value)
        case defn: Defn.Def =>
          createFunctionDeclarationTree(metaData, defn)
        case Term.Block(stats) =>
          new BlockTreeImpl(metaData, convert(stats))
        case Term.If(cond, thenp, elsep) =>
          createIfTree(metaData, cond, thenp, elsep)
        case matchTree: Term.Match =>
          createMatchTree(metaData, matchTree)
        case classDecl: Defn.Class =>
          val identifier = convert(classDecl.name).asInstanceOf[IdentifierTree]
          new ClassDeclarationTreeImpl(metaData, identifier, createNativeTree(metaData, classDecl))
        case v: Defn.Val if v.parent.exists(_.is[Template]) =>
          createNativeTree(metaData, metaTree)
        case v: Defn.Var if v.parent.exists(_.is[Template]) =>
          createNativeTree(metaData, metaTree)
        case Defn.Val(List(), List(Pat.Var(name)), decltpe, rhs) =>
          createVariableDeclarationTree(metaData, name, decltpe, convert(rhs), true)
        case Defn.Var(List(), List(Pat.Var(name)), decltpe, rhs) =>
          createVariableDeclarationTree(metaData, name, decltpe, convert(rhs).orNull, false)
        case _ =>
          createNativeTree(metaData, metaTree)
      }
    }

    private def treeMetaData(metaTree: Tree) = {
      metaDataProvider.metaData(textRange(metaTree))
    }

    private def createNativeTree(metaData: TreeMetaData, metaTree: Tree) = {
      val nativeKind = ScalaNativeKind(metaTree.getClass)
      new NativeTreeImpl(metaData, nativeKind, convert(metaTree.children))
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

    private def convert(optionalTree: Option[scala.meta.Tree]): Option[slang.api.Tree] = {
      optionalTree.map(t => convert(t))
    }

    private def createFunctionDeclarationTree(metaData: TreeMetaData, defn: Defn.Def): slang.api.Tree = {
      if (defn.paramss.size > 1) {
        return createNativeTree(metaData, defn)
      }
      val modifiers = convert(defn.mods)
      val returnType = defn.decltpe.map(convert).orNull
      val name = convert(defn.name).asInstanceOf[slang.api.IdentifierTree]
      val params = defn.paramss match {
        case List(x) => convert(x)
        case _ => emptyList[api.Tree]
      }
      val rawBody = convert(defn.body);
      val body = rawBody match {
        case b: slang.api.BlockTree => b
        case _ => new BlockTreeImpl(rawBody.metaData(), singletonList(rawBody))
      }
      val nativeChildren = convert(defn.tparams)
      new FunctionDeclarationTreeImpl(metaData, modifiers, returnType, name, params, body, nativeChildren)
    }

    private def createIfTree(metaData: TreeMetaData, cond: Term, thenp: Term, elsep: Term) = {
      val convertedCond = convert(cond)
      val convertedThenp = convert(thenp)
      val convertedElsep = convert(elsep)
      val ifKeyword = keyword(metaData.textRange.start, convertedCond.textRange.start)
      val elseKeyword = if (convertedElsep == null) null else keyword(convertedThenp.textRange.end, convertedElsep.textRange.start)
      new IfTreeImpl(metaData, convertedCond, convertedThenp, convertedElsep, ifKeyword, elseKeyword)
    }

    private def createMatchTree(metaData: TreeMetaData, matchTree: Term.Match): api.Tree = {
      if (matchTree.cases.exists(c => c.cond.nonEmpty)) {
        return createNativeTree(metaData, matchTree)
      }
      val convertedCases = matchTree.cases
        .map(c => new MatchCaseTreeImpl(treeMetaData(c), if (c.pat.is[Pat.Wildcard]) null else convert(c.pat), convert(c.body))
          .asInstanceOf[slang.api.MatchCaseTree])
      val convertedExpression = convert(matchTree.expr)
      val matchKeyword = keyword(convertedExpression.textRange.end, convertedCases.head.textRange.start)
      new MatchTreeImpl(metaData, convertedExpression, convertedCases.asJava, matchKeyword)
    }

    private def createVariableDeclarationTree(metaData: TreeMetaData, name: Term.Name, decltpe: Option[Type], rhs: slang.api.Tree, isVal: Boolean) = {
      val identifier = convert(name).asInstanceOf[slang.api.IdentifierTree]
      new VariableDeclarationTreeImpl(metaData, identifier, convert(decltpe).orNull, rhs, isVal)
    }

    private def keyword(start: slang.api.TextPointer, end: slang.api.TextPointer): slang.api.Token = {
      metaDataProvider.keyword(new TextRangeImpl(start, end))
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
