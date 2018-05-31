  x == 1;
  1 == 1; // Noncompliant {{Correct one of the identical sub-expressions on both sides this operator}}
  x
//^>
    == x; // Noncompliant
//     ^
  1 == 2;
  x = x;
  x + x;
  x * x;
  x <= x; // Noncompliant
