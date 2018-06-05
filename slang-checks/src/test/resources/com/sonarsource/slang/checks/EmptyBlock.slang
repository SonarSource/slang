void a() {}

void b() {
  // comment
}

void c(int x) {
  // Noncompliant@+1
  if (x > 0) { };

  if (x > 1) {
    // comment
  };

  match (x) {
  // comment
  };

  // Noncompliant@+1
  match (x) {

  };

  match (x) {
    // Noncompliant@+1
    1 -> { };
    2 -> x;
    else -> { x; };
  };
}