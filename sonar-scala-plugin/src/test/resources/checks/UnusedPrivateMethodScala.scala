class MyClass {
  private def unused() = { // Noncompliant
    used()
  }
  private def used() = {}

  def notPrivate() = {}

  // Serializable method should not raise any issue in Scala.
  private def writeObject() {}
  private def readObject() {}
  private def writeReplace() {}
  private def readResolve() {}
  private def readObjectNoData() {}
}
