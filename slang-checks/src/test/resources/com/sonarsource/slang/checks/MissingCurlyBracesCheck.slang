
if (condition) // Noncompliant {{Add curly braces around the nested statement(s) in this "if" block.}}
   doSomething();

void fun foo() {
   if (condition) doSomething(); // Noncompliant
// ^^

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

  // expressions
  x = if (condition) 1 else 2;

  x = if (condition)
         1
    else
         2;

  // lambdas are not yet supported by SLang, the coverage about "isLambda(...)" is done in KotlinSensorTest.test_missing_curly_braces_rule_in_lambda

}
