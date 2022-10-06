def usesString(using String): List[Int] = ???

val ints: List[Int] =
  for
    given String <- List("abc", "def", "xyz") // this now works
    result <- usesString
  yield results