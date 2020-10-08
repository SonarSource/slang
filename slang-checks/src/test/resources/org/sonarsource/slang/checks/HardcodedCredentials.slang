x = "pass";
"pass";
x = "password";
"password";
x = "login=a&password=";
"login=a&password=";
"login=a&password= " + value;
"login=a&password=a"; // Noncompliant
x = "login=a&password=xxx"; // Noncompliant {{"password" detected in this expression, review this potentially hardcoded credential.}}
//  ^^^^^^^^^^^^^^^^^^^^^^
"login=a&password=xxx"; // Noncompliant
"login=a&passwd=xxx"; // Noncompliant {{"passwd" detected in this expression, review this potentially hardcoded credential.}}
"login=a&pwd=xxx"; // Noncompliant {{"pwd" detected in this expression, review this potentially hardcoded credential.}}
  variableNameWithPasswordInIt = "xxx"; // Noncompliant {{"Password" detected in this expression, review this potentially hardcoded credential.}}
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^
variableNameWithPasswdInIt = "xxx"; // Noncompliant
variableNameWithPasswdInIt += "xxx"; // Noncompliant
variableNameWithPwdInIt = "xxx";  // Noncompliant {{"Pwd" detected in this expression, review this potentially hardcoded credential.}}
A.variableNameWithPwdInIt = "xxx";  // Noncompliant {{"Pwd" detected in this expression, review this potentially hardcoded credential.}}
A.B.variableNameWithPwdInIt = "xxx";  // Noncompliant {{"Pwd" detected in this expression, review this potentially hardcoded credential.}}
otherVariableNameWithPasswordInIt;
variableNameWithPasswordInIt = native[] { [ "NativeFunctionCall" ] };
val constValue = "login=a&password=xxx"; // Noncompliant
var passwd = "xxxx"; // Noncompliant
var okVariable = "xxxx";
variableNameWithPasswdInIt = "";
A.B.variableNameWithPwdInIt = "";
var passwd = "";
var passwd = 2;
