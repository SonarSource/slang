// this file is successfully parsed only with Scala 2 parser using scalameta_2.13 since:
// 1) Literal Types are supported in scalameta_2.13
// 2) Scala 3 drops do-while
class A extends {

  // Literal Types
  def f(): false = {
    // something
  }

  def fun(c : Boolean): Unit = {
    // do-while
    do {
      // soemthing
    } while (c)
  }
}