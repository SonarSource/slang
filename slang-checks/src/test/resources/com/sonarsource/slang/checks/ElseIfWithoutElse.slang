if (x == 0) {
  x = 42;
}

if (x == 0) {
  x = 42;
} else {
  x = -42;
}

if (x == 0) {
  doSomething();
} else if (x == 1) { // Noncompliant
//     ^[el=+3;ec=1]
  doSomethingElse();
}

if (x == 0) {
  doSomething();
} else if (x == 1) {
  doSomethingElse();
} else {
  print("Something");
}
