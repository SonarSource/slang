package org.sonarsource.scala.converter

import org.sonarsource.slang

import scala.meta._

class ScalaCodeVerifier extends slang.api.CodeVerifier {

  override def containsCode(content: String): Boolean = {
    val wrappedContent = "object Obj { def f1() = { " + content + " } }"
    val source: Source = wrappedContent.parse[Source] match {
      case scala.meta.parsers.Parsed.Success(t) => t
      case scala.meta.parsers.Parsed.Error(_, _, _) => return false
    }

    val body = source
      .stats.head
      .asInstanceOf[Defn.Object]
      .templ.stats.head
      .asInstanceOf[Defn.Def]
      .body

    body match {
      case Term.Block(statements) =>
        !statements.forall(isSimpleExpression) && areAllOnSeparateLines(statements)
      case _ => true
    }
  }

  private def areAllOnSeparateLines(statements: List[Tree]) = {
    !statements
      .groupBy(_.pos.startLine)
      .values
      .exists(_.size > 1)
  }

  private def isSimpleExpression(tree: Tree): Boolean = {
    tree match {
      case Term.New(Init(_, _, List())) => true
      case Term.Apply(name, arg::_) if name.pos.startLine == arg.pos.startLine && name.pos.endColumn < arg.pos.startColumn - 1 => true
      case Term.Assign(Term.Select(_,_), _) => true
      case Term.Return(expr) if isSimpleExpression(expr) => true
      case Term.Try(expr, List(), None) if isSimpleExpression(expr) => true
      case _ => (tree.is[Lit]
        || tree.is[Term.Name]
        || tree.is[Term.Select]
        || tree.is[Term.ApplyInfix]
        || tree.is[Term.ApplyType]
        || tree.is[Term.Ascribe]
        || tree.is[Term.Function]
        || tree.is[Term.Tuple])
    }
  }

}
