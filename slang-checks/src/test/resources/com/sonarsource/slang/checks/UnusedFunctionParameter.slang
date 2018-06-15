void fun fooBar() {}  // OK

void fun fooBar(int a) {}  // Noncompliant {{Remove this unused function parameter "a".}}
//                  ^

void fun fooBar(int a, int b) {}  // Noncompliant {{Remove these unused function parameters.}}
//                  ^      ^<  {{Remove this unused method parameter b".}}
//                  ^@-1<      {{Remove this unused method parameter a".}}

void fun fooBar(int a, int b) {   // Noncompliant {{Remove this unused function parameter "b".}}
//                         ^
  x = a;
}

void fun fooBar(int a, int b) {   // Noncompliant {{Remove this unused function parameter "a".}}
//                  ^
  match (b) { }
}

void fun fooBar(int a, b) {   // OK
  x = a;
  match (b) { }
}