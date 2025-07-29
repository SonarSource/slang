fun testFunction(a, b) { // Noncompliant {{Use slices.Equal() instead of custom slice comparison}}
//  ^^^^^^^^^^^^
  if (len(a) != len(b)) {
    return false;
  };
  return true;
}

fun normalFunction(x, y) {
  return x + y;
} 