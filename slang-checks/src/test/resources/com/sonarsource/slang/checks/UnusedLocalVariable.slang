int var global;       // Compliant

void fun fooBar() {
    int val a;        // Compliant
    int var b;        // Noncompliant {{Remove this unused "b" local variable.}}
    //      ^
    int var c;        // Compliant
    c = 0;
}
