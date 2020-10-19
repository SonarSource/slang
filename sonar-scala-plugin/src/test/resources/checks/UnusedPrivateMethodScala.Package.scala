package some.pack.name

class Foo {

  private def usedMethod(): Unit = { // ok, used in the companion object below
  }
}

object Foo {

  def bar(formats: Seq[Foo]): Unit = {
    for (inputSplit <- formats) {
      inputSplit.usedMethod()
    }
  }
}

class otherFoo {
  private def notUsed() : Unit = {} // Noncompliant
}
