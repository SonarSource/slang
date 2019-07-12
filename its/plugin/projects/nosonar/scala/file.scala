object A {
  def foo(): Unit = {
    val pwd = "secret"; // NOSONAR
    pwd = "secret";     // raise an issue S2068
  }
}
