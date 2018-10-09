// copyright
object Main {
  private val f1 : Integer => Integer = i => 1 + 1

  private def f2 : Integer => Integer = j => 1 + 1

  private def f3(i :Int): Integer = {
    1 + 1
  }

  private def f4(i :Int, j :Int, k :Int): Integer = {
    i + k
  }

  private def f5(i :Int, j :Int = 5, k :Int): Integer = {
    i + k
  }

  private def f6(implicit i : Int): Integer = {
    1
  }
}
