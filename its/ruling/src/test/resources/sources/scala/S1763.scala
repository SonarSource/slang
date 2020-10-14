// copyright
object Main {
  def main(args: Array[String]): Unit = {
    return
    print("Hello World")
  }

  def print(x: Int) = {
    throw new Exception()
    println("Will not execute")
  }
}
