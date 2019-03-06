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

  private def f(implicit p1: Int, p2: String) = {
    g // p1 and p2 are used implicitly by this 'g' function call
  }

  private def g(implicit p1: Int, p2: String) = {
    print(p1)
    print(p2)
  }
}
