class OuterClass {
  private def unused() = { // Noncompliant
    used()
  }

  private def used() = {}

  class MyClass {
    private def unused() = { // Noncompliant
      used()
    }

    private def used() = {}
  }
}