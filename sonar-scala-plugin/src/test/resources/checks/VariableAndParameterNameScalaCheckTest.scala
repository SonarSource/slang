import scala.collection.parallel.mutable.ParMap

private object Guardian {

  // Quotes is considered a parameter without an identifier
  def guard()(using Quotes): Expr[Unit] = null

  def unguard(OBJECT: Object): Expr[Unit] = null // Noncompliant {{Rename this parameter to match the regular expression "^[_a-z][a-zA-Z0-9]*$".}}
  //          ^^^^^^

  def init(param: Object): Unit = null
}