170;

   0252; // Noncompliant {{Use decimal values instead of octal ones.}}
// ^^^^
0o252; // Noncompliant
0O252; // Noncompliant

"0o252";
"0252";
"0O252";

a + 02; // Noncompliant
//  ^^
