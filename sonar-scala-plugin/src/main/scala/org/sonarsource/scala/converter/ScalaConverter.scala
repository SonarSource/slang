/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import java.util
import java.util.Collections.{emptyList, singletonList}

import javax.annotation.Nullable
import org.sonarsource.slang
import org.sonarsource.slang.api
import org.sonarsource.slang.api.{ASTConverter, Annotation, BinaryExpressionTree, CatchTree, IdentifierTree, TextRange, Token, TreeMetaData, UnaryExpressionTree}
import org.sonarsource.slang.api.LoopTree.LoopKind
import org.sonarsource.slang.impl._

import scala.collection.JavaConverters._
import scala.meta._
import scala.meta.internal.tokenizers.keywords
import scala.meta.parsers.Parsed.{Error, Success}
import scala.meta.tokens.Token.{CR, Comment, LF, Space, Tab}

class ScalaConverter extends ASTConverter {
  val BINARY_OPERATOR_MAP = Map(
    "+" -> BinaryExpressionTree.Operator.PLUS,
    "-" -> BinaryExpressionTree.Operator.MINUS,
    "*" -> BinaryExpressionTree.Operator.TIMES,
    "/" -> BinaryExpressionTree.Operator.DIVIDED_BY,

    "==" -> BinaryExpressionTree.Operator.EQUAL_TO,
    "!=" -> BinaryExpressionTree.Operator.NOT_EQUAL_TO,
    ">"  -> BinaryExpressionTree.Operator.GREATER_THAN,
    ">=" -> BinaryExpressionTree.Operator.GREATER_THAN_OR_EQUAL_TO,
    "<"  -> BinaryExpressionTree.Operator.LESS_THAN,
    "<=" -> BinaryExpressionTree.Operator.LESS_THAN_OR_EQUAL_TO,

    "&&" -> BinaryExpressionTree.Operator.CONDITIONAL_AND,
    "||" -> BinaryExpressionTree.Operator.CONDITIONAL_OR
  )

  val UNARY_OPERATOR_MAP = Map(
    "!"  -> UnaryExpressionTree.Operator.NEGATE,
    "+"  -> UnaryExpressionTree.Operator.PLUS,
    "-"  -> UnaryExpressionTree.Operator.MINUS
  )

  override def parse(code: String, @Nullable fileName: String): slang.api.Tree = {

    scala.meta.internal.tokenizers.PlatformTokenizerCache.megaCache.clear()

    try {
      val metaTree: scala.meta.Tree = code.parse[Source] match {
        case Success(tree) => tree
        case Error(pos, _, _) =>
          dialects.Sbt1(code).parse[Source] match {
            case Success(t) => t
            case Error(_, _, _) =>
              throw new slang.api.ParseException("Unable to parse file content.", textRange(pos).start())
          }
      }
      val allTokens = metaTree.tokens
        .filter(t => t.isNot[Comment])
        .filter(t => t.pos.start < t.pos.end && t.isNot[Space] && t.isNot[Tab] && t.isNot[CR] && t.isNot[LF])
        .map(t => new TokenImpl(textRange(t.pos), t.text, tokenType(t)))
        .asInstanceOf[IndexedSeq[Token]]
        .asJava

      val allComments = metaTree.tokens
        .filter(t => t.is[Comment])
        .map(t => createComment(t))
        .asJava

      val metaDataProvider = new TreeMetaDataProvider(allComments, allTokens, collectAnnotations(metaTree).asJava)
      new TreeConversion(metaDataProvider).convert(metaTree)
    } catch {
      case e: org.scalameta.UnreachableError => throw new slang.api.ParseException("Unable to parse file content.", null, e)
    }
  }

  override def parse(code: String): slang.api.Tree = {
    parse(code, fileName = null)
  }

  private def collectAnnotations(tree: Tree): List[Annotation] = tree match {
    case Mod.Annot(init) =>
      List(new AnnotationImpl(getShortName(init.tpe), init.argss.flatten.map(_.toString()).asJava, textRange(tree.pos)))
    case _ => tree.children.flatMap(collectAnnotations)
  }

  private def getShortName(tpe: Type): String = tpe match {
    case Type.Select(_, name) => name.toString()
    case _ => tpe.toString()
  }

  private class TreeConversion(metaDataProvider: TreeMetaDataProvider) {

    def convert(metaTree: scala.meta.Tree): slang.api.Tree = {
      metaTree match {
        case implicitTree: Mod.Implicit => return convertModImplicit(implicitTree)
        case _ =>
      }
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
        case lit: scala.meta.Term.Interpolate if lit.args.nonEmpty =>
          // We don't transform the parts in StringLiteralTreeImpl if we have some args in the interpolated term
          // Example: s"abs $x" is not transformed but raw"abc" is
          mapInterpolateWithArgs(metaTree, metaData, lit)
        case Term.Name(value) =>
          new IdentifierTreeImpl(metaData, value)
        case Type.Name(value) =>
          new IdentifierTreeImpl(metaData, value)
        case defn: Defn.Def =>
          createFunctionDeclarationTree(metaData, defn)
        case ctor: Ctor.Primary =>
          createPrimaryConstructorDeclaration(metaData, ctor)
        case ctor: Ctor.Secondary =>
          createSecondaryConstructorDeclaration(metaData, ctor)
        case Term.Block(stats) =>
          new BlockTreeImpl(metaData, convert(stats))
        case Term.Assign(lhs, rhs) if metaTree.parent.exists(p => p.isNot[Term.Apply] && p.isNot[Init]) =>
          new AssignmentExpressionTreeImpl(metaData, slang.api.AssignmentExpressionTree.Operator.EQUAL, convert(lhs), convert(rhs));
        case Term.If(cond, thenp, elsep) =>
          createIfTree(metaData, cond, thenp, elsep)
        case Term.While(expr, body) =>
          val convertedExpr = convert(expr)
          new LoopTreeImpl(metaData, convertedExpr, convert(body), LoopKind.WHILE, keyword(metaData.textRange.start, start(convertedExpr)))
        case Term.Do(body, expr) =>
          val convertedBody = convert(body)
          new LoopTreeImpl(metaData, convert(expr), convertedBody, LoopKind.DOWHILE, keyword(metaData.textRange.start, start(convertedBody)))
        case Term.For(enums, body) =>
          val convertedEnums = createNativeTree(enums, ScalaForConditionKind)
          new LoopTreeImpl(metaData, convertedEnums, convert(body), LoopKind.FOR, keyword(metaData.textRange.start, start(convertedEnums)))
        case matchTree: Term.Match =>
          createMatchTree(metaData, matchTree)
        case classDecl: Defn.Class =>
          val identifier = convert(classDecl.name).asInstanceOf[IdentifierTree]
          new ClassDeclarationTreeImpl(metaData, identifier, createNativeTree(metaData, classDecl))
        case v: Defn.Val if grandParentIsNewAnonymous(v) =>
          createNativeTree(metaData, metaTree)
        case v: Defn.Var if grandParentIsNewAnonymous(v) =>
          createNativeTree(metaData, metaTree)
        case Defn.Val(List(), List(Pat.Var(name)), decltpe, rhs) =>
          createVariableDeclarationTree(metaData, name, decltpe, convert(rhs), isVal = true)
        case Defn.Var(List(), List(Pat.Var(name)), decltpe, rhs) =>
          createVariableDeclarationTree(metaData, name, decltpe, convert(rhs).orNull, isVal = false)
        case infix: Term.ApplyInfix =>
          BINARY_OPERATOR_MAP.get(infix.op.value) match {
            case Some(operator) => createBinaryExpressionTree(metaData, infix, operator)
            case None => createNativeTree(metaData, infix)
          }
        case unaryExpression: Term.ApplyUnary =>
          UNARY_OPERATOR_MAP.get(unaryExpression.op.value) match {
            case Some(operator) => new UnaryExpressionTreeImpl(metaData, operator, convert(unaryExpression.arg))
            case None => createNativeTree(metaData, unaryExpression)
          }
        case Term.Try(expr, catchp, finallyp) =>
          val catchBlock = Some(catchp).filter(_.nonEmpty).map(createNativeTree(_, ScalaCatchBlockKind))
          createExceptionHandlingTree(metaData, expr, catchBlock, finallyp)
        case Term.TryWithHandler(expr, catchp, finallyp) =>
          createExceptionHandlingTree(metaData, expr, Some(convert(catchp)), finallyp)
        case Mod.Private(within) if isStrictPrivate(within) =>
          new ModifierTreeImpl(metaData, slang.api.ModifierTree.Kind.PRIVATE)
        case Mod.Override() =>
          new ModifierTreeImpl(metaData, slang.api.ModifierTree.Kind.OVERRIDE)
        case Term.Return(expr) =>
          createReturnTree(metaData, expr)
        case Term.Placeholder() =>
          new PlaceHolderTreeImpl(metaData, metaDataProvider.keyword(metaData.textRange()))
        case Term.Throw(expr) =>
          createThrowTree(metaData, expr)
        case _ =>
          createNativeTree(metaData, metaTree)
      }
    }

    def grandParentIsNewAnonymous(variable: Stat) = {
      variable.parent.exists(p => p.is[Template] && p.parent.exists(gp => gp.is[Term.NewAnonymous]))
    }

    def convertModImplicit(metaTree: Mod.Implicit): slang.api.Tree = {
      // Implicit modifier position is broken in Scalameta, we need to retrieve
      // "implicit" token ourselves (https://github.com/scalameta/scalameta/issues/1132)
      val metaData = treeMetaData(metaTree.parent.get)
      var previousToken: java.util.Optional[Token] = metaDataProvider.previousToken(metaData.textRange)
      while (previousToken.isPresent && !previousToken.get.text.equals("implicit")) {
        previousToken = metaDataProvider.previousToken(previousToken.get.textRange)
      }
      if (!previousToken.isPresent) {
        return null
      }
      new NativeTreeImpl(metaDataProvider.metaData(previousToken.get.textRange), ScalaImplicitKind, List().asJava)
    }

    private def createReturnTree(metaData: TreeMetaData, expr: Term) = {
      val convertedExpr = convert(expr)
      new ReturnTreeImpl(metaData, createKeyword(metaData, convertedExpr), convertedExpr)
    }

    private def createThrowTree(metaData: TreeMetaData, expr: Term) = {
      val convertedExpr = convert(expr)
      new ThrowTreeImpl(metaData, createKeyword(metaData, convertedExpr), convertedExpr)
    }

    private def createKeyword(metaData: TreeMetaData, expr: slang.api.Tree) = {
      val end = if (expr == null) metaData.textRange.end else start(expr)
      keyword(metaData.textRange.start, end)
    }

    // private => like Java
    // private[this] =>  accessible only for current instance
    // private[foo] => accessible inside foo package
    private def isStrictPrivate(within: Ref) = {
      within.isInstanceOf[Name.Anonymous] || within.isInstanceOf[Term.This]
    }

    private def createExceptionHandlingTree(metaData: TreeMetaData, expr: Term, catchBlock: Option[slang.api.Tree], finallyp: Option[Term]) = {
      val convertedExpr = convert(expr)
      val tryKeyword = keyword(metaData.textRange.start, start(convertedExpr))
      val catchTrees = catchBlock match {
        case Some(b) => singletonList(createCatchTree(convertedExpr, b))
        case None => emptyList[CatchTree]()
      }
      new ExceptionHandlingTreeImpl(metaData, convertedExpr, tryKeyword, catchTrees, convert(finallyp).orNull)
    }

    private def createCatchTree(convertedExpr: slang.api.Tree, catchBlock: slang.api.Tree): CatchTree = {
      val catchKeyword = keyword(convertedExpr.textRange.end, start(catchBlock))
      val catchMetaData = metaDataProvider.metaData(new TextRangeImpl(convertedExpr.textRange.end, catchBlock.textRange.end))
      new CatchTreeImpl(catchMetaData, null, catchBlock, catchKeyword)
    }

    private def treeMetaData(metaTree: Tree) = {
      metaDataProvider.metaData(textRange(metaTree))
    }

    private def createNativeTree(children: List[scala.meta.Tree], nativeKind: slang.api.NativeKind) = {
      val convertedChildren = convert(children)
      val lastConvertedChild = convertedChildren.get(convertedChildren.size() - 1)
      val metaData = metaDataProvider.metaData(new TextRangeImpl(convertedChildren.get(0).textRange.start, lastConvertedChild.textRange.end))
      new NativeTreeImpl(metaData, nativeKind, convertedChildren)
    }

    private def createArtificialBlockTree(children: List[scala.meta.Tree]) : BlockTreeImpl = {
      if (children == null || children.isEmpty) {
        return null
      }
      val convertedChildren = convert(children)
      val lastConvertedChild = convertedChildren.get(convertedChildren.size() - 1)
      val metaData = metaDataProvider.metaData(new TextRangeImpl(convertedChildren.get(0).textRange.start, lastConvertedChild.textRange.end))
      new BlockTreeImpl(metaData, convertedChildren)
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

    private def mapInterpolateWithArgs(metaTree: Tree, metaData: TreeMetaData, lit: scala.meta.Term.Interpolate) = {
      val nativeKind = ScalaNativeKind(metaTree.getClass)
      val convertedChildren = new util.ArrayList[slang.api.Tree]()

      convertedChildren.add(convert(lit.prefix))

      lit.parts.filter(p => p.pos.start < p.pos.end)
        .map(p => createNativeTree(metaDataProvider.metaData(textRange(p)), p))
        .foreach(convertedChildren.add)

      lit.args.map(convert)
        .filter(_ != null)
        .foreach(convertedChildren.add)

      new NativeTreeImpl(metaData, nativeKind, convertedChildren)
    }

    private def convert(trees: scala.List[scala.meta.Tree]): java.util.List[slang.api.Tree] = {
      trees.map(convert).filter(_ != null).asJava
    }

    private def convert(optionalTree: Option[scala.meta.Tree]): Option[slang.api.Tree] = {
      optionalTree.map(convert)
    }

    private def createFunctionDeclarationTree(metaData: TreeMetaData, defn: Defn.Def): slang.api.Tree = {
      val modifiers = convert(defn.mods)
      val returnType = defn.decltpe.map(convert).orNull
      val name = convert(defn.name).asInstanceOf[slang.api.IdentifierTree]
      val allParams = defn.paramss.flatten.map(createParameterTree).asJava
      val rawBody = convert(defn.body)
      val body = rawBody match {
        case b: slang.api.BlockTree => b
        case _ => new BlockTreeImpl(rawBody.metaData(), singletonList(rawBody))
      }
      val nativeChildren = convert(defn.tparams)
      new FunctionDeclarationTreeImpl(metaData, modifiers, false, returnType, name, allParams, body, nativeChildren)
    }

    private def createPrimaryConstructorDeclaration(metaData: TreeMetaData, ctor: Ctor.Primary): slang.api.Tree = {
      val modifiers = convert(ctor.mods)
      val isConstructor = true
      val returnType = null
      val name = convert(ctor.name).asInstanceOf[slang.api.IdentifierTree]
      val allParams = ctor.paramss.flatten.map(createParameterTree).asJava
      val body = null
      val nativeChildren = emptyList[slang.api.Tree]()
      new FunctionDeclarationTreeImpl(metaData, modifiers, isConstructor, returnType, name, allParams, body, nativeChildren)
    }

    private def createSecondaryConstructorDeclaration(metaData: TreeMetaData, ctor: Ctor.Secondary): slang.api.Tree = {
      val modifiers = convert(ctor.mods)
      val isConstructor = true
      val returnType = null
      val name = null
      val allParams = ctor.paramss.flatten.map(createParameterTree).asJava
      var body = createArtificialBlockTree(ctor.stats)
      if (body == null) {
        body = createArtificialBlockTree(List(ctor.init))
      }
      val nativeChildren = emptyList[slang.api.Tree]()
      new FunctionDeclarationTreeImpl(metaData, modifiers, isConstructor, returnType, name, allParams, body, nativeChildren)
    }

    private def createParameterTree(param: Term.Param): slang.api.Tree = {
      val identifier = convert(param.name).asInstanceOf[slang.api.IdentifierTree]
      val typ = convert(param.decltpe).orNull
      val defaultValue = convert(param.default).orNull
      val modifiers = convert(param.mods)
      new ParameterTreeImpl(treeMetaData(param), identifier, typ, defaultValue, modifiers)
    }

    private def createIfTree(metaData: TreeMetaData, cond: Term, thenp: Term, elsep: Term) = {
      val convertedCond = convert(cond)
      val convertedThenp = convert(thenp)
      val convertedElsep = convert(elsep)
      val ifKeyword = keyword(metaData.textRange.start, start(convertedCond))
      val elseKeyword = if (convertedElsep == null) null else keyword(convertedThenp.textRange.end, start(convertedElsep))
      new IfTreeImpl(metaData, convertedCond, convertedThenp, convertedElsep, ifKeyword, elseKeyword)
    }

    private def createMatchTree(metaData: TreeMetaData, matchTree: Term.Match): api.Tree = {
      val convertedCases = matchTree.cases.map(createCaseTree)
      val convertedExpression = convert(matchTree.expr)
      val matchKeyword = keyword(convertedExpression.textRange.end, start(convertedCases.head))
      new MatchTreeImpl(metaData, convertedExpression, convertedCases.asJava, matchKeyword)
    }

    private def createCaseTree(c: Case): api.MatchCaseTree =
      new MatchCaseTreeImpl(treeMetaData(c), convertCaseExpression(c), convert(c.body)).asInstanceOf[slang.api.MatchCaseTree]
    

    private def convertCaseExpression(c: Case): api.Tree = {
      c.cond match {
        case Some(cond) => createNativeTree(List(c.pat, cond), ScalaCaseWithConditionKind)
        // default case
        case None if c.pat.is[Pat.Wildcard] => null
        case None => convert(c.pat)
      }
    }

    private def createVariableDeclarationTree(metaData: TreeMetaData, name: Term.Name, decltpe: Option[Type], rhs: slang.api.Tree, isVal: Boolean) = {
      val identifier = convert(name).asInstanceOf[slang.api.IdentifierTree]
      new VariableDeclarationTreeImpl(metaData, identifier, convert(decltpe).orNull, rhs, isVal)
    }

    private def createBinaryExpressionTree(metaData: TreeMetaData, infix: Term.ApplyInfix, operator: BinaryExpressionTree.Operator): slang.api.Tree = {
      // Scala can have multiple arguments on the right-hand side of an infix function application
      // Example: foo ** (bar, baz)
      if (infix.args.length != 1) {
        return createNativeTree(metaData, infix)
      }
      val leftOperand = convert(infix.lhs)
      val rightOperand = convert(infix.args.head)
      val operatorToken = treeMetaData(infix.op).tokens().get(0)
      new BinaryExpressionTreeImpl(metaData, operator, operatorToken, leftOperand, rightOperand)
    }

    private def keyword(start: slang.api.TextPointer, end: slang.api.TextPointer): slang.api.Token = {
      metaDataProvider.keyword(new TextRangeImpl(start, end))
    }
  }

  case class ScalaNativeKind(treeClass: Class[_ <: scala.meta.Tree]) extends slang.api.NativeKind {
  }

  object ScalaCatchBlockKind extends slang.api.NativeKind
  object ScalaForConditionKind extends slang.api.NativeKind
  object ScalaImplicitKind extends slang.api.NativeKind
  object ScalaCaseWithConditionKind extends slang.api.NativeKind

  def tokenType(token: scala.meta.tokens.Token): Token.Type = {
    if (token.is[scala.meta.tokens.Token.Constant.String]) {
      return Token.Type.STRING_LITERAL
    }
    if (keywords(dialects.Scala3).contains(token.text)) {
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
    val treePosition = tree.pos
    tree.children match {
      // We've seen this happen in the case of MatchCaseTree blocks.
      // To avoid ending on column 0, we make a compromise on ending where the last child ends.
      // The downside of this approach is that there could be other tokens after the last child (e.g. curly brace), which we ignore.
      case _ :+ last if treePosition.endLine != last.pos.endLine =>
        TextRanges.range(treePosition.startLine + 1, treePosition.startColumn, last.pos.endLine + 1, last.pos.endColumn)
      case _ =>
        textRange(treePosition)
    }
  }

  def textRange(pos: scala.meta.Position): TextRange = {
    TextRanges.range(pos.startLine + 1, pos.startColumn, pos.endLine + 1, pos.endColumn)
  }

  private def start(tree: slang.api.Tree): slang.api.TextPointer = {
    tree.textRange.start
  }

}
