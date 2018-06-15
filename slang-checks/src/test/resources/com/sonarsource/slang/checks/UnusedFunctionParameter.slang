void fun fooBar() {}  // OK

void fun fooBar(int a) {}  // Noncompliant {{Remove this unused function parameter "a".}}
//              ^^^^^

void fun fooBar(int a, int b) {}  // Noncompliant {{Remove these unused function parameters.}}
//              ^^^^^  ^^^^^<

void fun fooBar(int a, int b) {   // Noncompliant {{Remove this unused function parameter "b".}}
//                     ^^^^^
  x = a;
}

void fun fooBar(int a, int b) {   // Noncompliant {{Remove this unused function parameter "a".}}
//              ^^^^^
  match (b) { }
}

void fun fooBar(int a, int b) {   // OK
  x = a;
  match (b) { }
}
