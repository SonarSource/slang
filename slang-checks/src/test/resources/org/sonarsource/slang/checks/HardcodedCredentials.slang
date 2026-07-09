x = "pass";
"pass";
x = "password";
"password";
x = "login=a&password=";
"login=a&password=";
"login=a&password= " + value;
"login=a&password=Ag4x9K2mP7nQ"; // Noncompliant
x = "login=a&password=Ag4x9K2mP7nQ"; // Noncompliant {{"password" detected here, make sure this is not a hard-coded credential.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
"login=a&password=Ag4x9K2mP7nQ"; // Noncompliant
"login=a&passwd=Ag4x9K2mP7nQ"; // Noncompliant {{"passwd" detected here, make sure this is not a hard-coded credential.}}
"login=a&pwd=Ag4x9K2mP7nQ"; // Noncompliant {{"pwd" detected here, make sure this is not a hard-coded credential.}}
"login=a&passphrase=Ag4x9K2mP7nQ"; // Noncompliant {{"passphrase" detected here, make sure this is not a hard-coded credential.}}
  variableNameWithPasswordInIt = "Ag4x9K2mP7nQ"; // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^
variableNameWithPasswdInIt = "Ag4x9K2mP7nQ"; // Noncompliant
variableNameWithPasswdInIt += "Ag4x9K2mP7nQ"; // Noncompliant
variableNameWithPwdInIt = "Ag4x9K2mP7nQ";  // Noncompliant {{"Pwd" detected here, make sure this is not a hard-coded credential.}}
A.variableNameWithPwdInIt = "Ag4x9K2mP7nQ";  // Noncompliant {{"Pwd" detected here, make sure this is not a hard-coded credential.}}
A.B.variableNameWithPwdInIt = "Ag4x9K2mP7nQ";  // Noncompliant {{"Pwd" detected here, make sure this is not a hard-coded credential.}}
otherVariableNameWithPasswordInIt;
variableNameWithPasswordInIt = native[] { [ "NativeFunctionCall" ] };
val constValue = "login=a&password=Ag4x9K2mP7nQ"; // Noncompliant
var passwd = "Ag4x9K2mP7nQ"; // Noncompliant
var passphrase = "Ag4x9K2mP7nQ"; // Noncompliant
var okVariable = "xxxx";
variableNameWithPasswdInIt = "";
A.B.variableNameWithPwdInIt = "";
var passwd = "";
var passwd = 2;

// SecretClassifier: known placeholder values are compliant
variableNameWithPasswordInIt = "xxx"; // Compliant
var passwd = "xxxx"; // Compliant
"login=a&password=xxx"; // Compliant

// SecretClassifier receives the full remainder after '=', including any trailing query params.
// Known limitation: a keyword in trailing params (e.g. "admin") can cause a false negative.
"password=xxx&user=admin"; // Compliant
"password=Ag4x9K2mP7nQ&user=admin"; // Compliant - FN: "admin" in suffix causes SecretClassifier to suppress

// No issue is raised when the matched wordlist item is present in both symbol name and literal string value.
var password = "password"; // Compliant
var myPassword = "users/connection.secretPassword"; // Compliant
myPassword = "users/connection.secretPassword"; // Compliant
var myPassword = "Ag4x9K2mP7nQ"; // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
myPassword = "Ag4x9K2mP7nQ"; // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
var params = "user=admin&password=Ag4x9K2mP7nQ"; // Noncompliant {{"password" detected here, make sure this is not a hard-coded credential.}}

// SecretClassifier: values containing credential-like words are compliant
var myPassword = "secretPasswd"; // Compliant
myPassword = "secretPasswd"; // Compliant
var params = "user=admin&password=Password123"; // Compliant

// Database queries are compliant
var query = "password=?";
query = "password=:password";
query = "password=:param";
query = "password='" + password + "'";
query = "password=%s";
query = "password=%v";

// String format is compliant
query = "password={0}";

// Support URI
var uri = "http://user:azer:ty123@domain.com"; // Noncompliant
var uri = "https://:azerty123@domain.com/path"; // Noncompliant {{Review this hard-coded URL, which may contain a credential.}}
var uri = "http://anonymous:anonymous@domain.com"; // Compliant, user and password are the same
var uri = "http://user:@domain.com";
var uri = "http://user@domain.com:80";
var uri = "http://domain.com/user:azerty123";
var uri = "too-long-url-scheme://user:123456@server.com";
var uri = "https:// invalid::url::format";
