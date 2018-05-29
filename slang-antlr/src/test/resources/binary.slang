// Example method in slang 
a = 2;
b = K + 1;
c = a - a;
d = "hello world";
e = 'c' + 'h' + 'a' + 'r';
f = true && false;
g = f;
native ["DEFER"] {
  [ a = 1; ]
  [ b = 2;
    c = b + 1;
  ]
}
