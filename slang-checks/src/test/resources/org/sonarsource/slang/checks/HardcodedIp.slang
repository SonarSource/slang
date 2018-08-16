x = 120;
"120";
ip = "1.2.3.4"; // Noncompliant {{Make this IP "1.2.3.4" address configurable.}}
//   ^^^^^^^^^
"1.2.3.4"; // Noncompliant {{Make this IP "1.2.3.4" address configurable.}}

// Noncompliant@+1 {{Make this IP "192.168.0.1" address configurable.}}
url = "http://192.168.0.1/admin.html";
url2 = "http://www.example.org";

notAnIp1 = "0.0.0.1234";
notAnIp2 = "1234.0.0.0";
notAnIp3 = "1234.0.0.0.0.1234";
notAnIp4 = ".0.0.0.0";
notAnIp5 = "0.256.0.0";

fileName = "v0.0.1.200__do_something.sql"; // Compliant - suffixed and prefixed
version = "1.0.0.0-1"; // Compliant - suffixed

// Exceptions

"0.0.0.0";
"255.255.255.255";
"2.5.255.255";
"127.5.255.255";
