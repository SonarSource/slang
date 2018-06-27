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
}
