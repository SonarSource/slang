class MyClass {
  private def foo() = {
    val name = "James"
    println(s"Hello, $name")  // is used

    val value = 1
    println(s"1 + 1 = ${value + 1}") // is used

    val height = 1.9d
    val person = "James"
    println(f"$person%s is $height%2.2f meters tall")  // is used

    val unused = 2; // Noncompliant
  }
}
