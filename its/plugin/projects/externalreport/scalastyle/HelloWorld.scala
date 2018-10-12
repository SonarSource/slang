import org.sonarsource.slang

object HelloWorld {
  def main(args: Array[String]): Unit = {
    if (args.isInstanceOf[Array[String]]) {
      println("Hello, world!")
    }
  }
  case class ScalaNativeKind() {
  }
}
