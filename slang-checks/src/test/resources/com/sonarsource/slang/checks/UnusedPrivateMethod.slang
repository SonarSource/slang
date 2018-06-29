class A {
  private fun unusedPrivate() {} // Noncompliant

  private fun unusedPrivate(int a) {} // Noncompliant

  private fun usedPrivate(int a) {}

  fun unusedNoModifier(int a) {
    usedPrivate();
  }

  public fun unusedPublic(int a) {}
}

class B {
    private fun f1() {}

    fun g(int f1) {} // FN of current approach, "f1" is used as identifier elsewhere in the class

    private fun f2() {}

    fun h(C c) { native[] {  [c;][f2();] } } // FN of current approach, another class has same identifier for a method call "f2" (c.f2();)

    private fun writeObject() {} // OK - this is ignored in default rule implementation for now (Serializable in Kotlin)
}

class C {
    private fun unusedPrivate1() {}  // Noncompliant

    private fun unusedPrivate2() {}  // FN of current approach, "unusedPrivate2" identifier is used in inner class

    private fun f1() {
      f1();
    }

    class D {
        private fun unusedPrivate2() {}  // Noncompliant

        private fun unusedPrivate3() {}  // Noncompliant

        private fun f1() {}  // Noncompliant
    }
}