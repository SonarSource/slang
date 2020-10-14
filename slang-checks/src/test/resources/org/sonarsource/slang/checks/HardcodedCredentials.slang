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
"login=a&passphrase=xxx"; // Noncompliant {{"passphrase" detected in this expression, review this potentially hardcoded credential.}}
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
var passphrase = "xxx"; // Noncompliant
var okVariable = "xxxx";
variableNameWithPasswdInIt = "";
A.B.variableNameWithPwdInIt = "";
var passwd = "";
var passwd = 2;

// No issue is raised when the matched wordlist item is present in both symbol name and literal string value.
var password = "password"; // Compliant
var myPassword = "users/connection.secretPassword"; // Compliant
myPassword = "users/connection.secretPassword"; // Compliant
var myPassword = "secretPasswd"; // Noncompliant {{"Password" detected in this expression, review this potentially hardcoded credential.}}
myPassword = "secretPasswd"; // Noncompliant {{"Password" detected in this expression, review this potentially hardcoded credential.}}
var params = "user=admin&password=Password123"; // Noncompliant {{"password" detected in this expression, review this potentially hardcoded credential.}}

// Database queries are compliant
var query = "password=?";
query = "password=:password";
query = "password=:param";
query = "password='" + password + "'";
query = "password=%s";
query = "password=%v";
