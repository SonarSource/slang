  a && b;
  a && b || c;
  a && b || c && d;
  a && b || c && d || e; // Noncompliant {{Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).}}
  a && b || c && d || e && f; // Noncompliant
//^^^^^^^^^^^^^^^^^^^^^^^^^^
if (a && b || c && d || e && f) {}; // Noncompliant
if (a && (b || c) && (d || e && f)) {}; // Noncompliant
if (a && b || c) {};
if (!(a && b || c && d)) {};
if (!(a && b || c && d || e)) {}; // Noncompliant