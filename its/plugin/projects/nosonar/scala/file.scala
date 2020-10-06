object A {
  def foo(): Unit = {
    if (true) {  // NOSONAR
    }
    
    if (true) {  // raise an issue S1145
    }
  }
}
