fun f1() {
  a; a;
}

fun f1() { // Noncompliant {{This function has 4 lines of code, which is greater than the 3 authorized. Split it into smaller functions.}}
//  ^^
  a;
  a;
}

fun f1() {

  a;
}

fun f1() {
  // comment
  a;
}
