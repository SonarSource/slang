
if (condition) // Noncompliant {{Add curly braces around the nested statement(s) in this "if" block.}}
   doSomething();

void fun foo() {
   if (condition) doSomething();

   if (condition) // Noncompliant {{Add curly braces around the nested statement(s) in this "if" block.}}
// ^^
     doSomething();

  if (condition) // Noncompliant
//^^
     doSomething()
  else // Noncompliant
//^^^^
     doSomethingElse();

  if (condition) {
    if (condition) {
    } else // Noncompliant
  //  ^^^^
      doSomething();
  } else {
  };

  if (condition) {
  };

  if (condition) {
  } else if (condition) {
  };

  for (int var x = list) // Noncompliant
//^^^
    doSomething();

  for (int var x = list) {
  };

  while (condition) // Noncompliant
//^^^^^
     doSomething();

  while (condition) {
  };

  do // Noncompliant
//^^
    something()
  while (false);

  do {
    something();
  } while (condition);
}
