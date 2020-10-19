class MyClass {
  private def unused() = { // Noncompliant
    used()
  }
  private def used() = {}

  private def usedByCompanionObject(): Unit = {} // ok, used in the companion object below

  def notPrivate() = {}

  // Serializable method should not raise any issue in Scala.
  private def writeObject() {}
  private def readObject() {}
  private def writeReplace() {}
  private def readResolve() {}
  private def readObjectNoData() {}
}

object MyClass {
  def apply(name: String): MyClass = {
    var p = new MyClass
    p.usedByCompanionObject()
    p
  }
}

class OtherClass {}

object OtherObject {
  def apply(name: String): OtherClass = {
    // for code coverage
    new OtherClass
  }
}
