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

class A {

  void fun fooBar() {}

  void fun fooBar(int a, int b) { // OK - method is not private and could potentially be overridden
    x = a;
  }

  private void fun fooBar(int a, int b) { // Noncompliant
    x = a;
  }

}