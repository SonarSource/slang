x == true;                                   // Noncompliant {{Remove the unnecessary Boolean literal.}}
//   ^^^^
x == false;                                  // Noncompliant {{Remove the unnecessary Boolean literal.}}
//   ^^^^^
x != true;                                   // Noncompliant {{Remove the unnecessary Boolean literal.}}
x != false;                                  // Noncompliant {{Remove the unnecessary Boolean literal.}}
true == x;                                   // Noncompliant {{Remove the unnecessary Boolean literal.}}
false == x;                                  // Noncompliant {{Remove the unnecessary Boolean literal.}}
true != x;                                   // Noncompliant {{Remove the unnecessary Boolean literal.}}
false != x;                                  // Noncompliant {{Remove the unnecessary Boolean literal.}}
!true;                                       // Noncompliant {{Remove the unnecessary Boolean literal.}}
!false;                                      // Noncompliant {{Remove the unnecessary Boolean literal.}}
false && foo;                                // Noncompliant {{Remove the unnecessary Boolean literal.}}
x || true;                                   // Noncompliant {{Remove the unnecessary Boolean literal.}}

!x;                                          // OK
x || foo;                                    // OK
x == y;                                      // OK
z != x;                                      // OK

x = if (foo) y else false;                   // Noncompliant {{Remove the unnecessary Boolean literal.}}
x = if (foo) y else true;                    // Noncompliant {{Remove the unnecessary Boolean literal.}}
x = if (foo) true else y;                    // Noncompliant {{Remove the unnecessary Boolean literal.}}
x = if (foo) false else y;                   // Noncompliant {{Remove the unnecessary Boolean literal.}}
x = if (foo) false;                          // Noncompliant {{Remove the unnecessary Boolean literal.}}

x = if (foo) x else y;                       // OK