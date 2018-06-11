void fooBar() {} // OK

void foo_bar() {} // Noncompliant {{Rename function foo_bar to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//   ^^^^^^^