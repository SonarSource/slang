// S122
package samples

func foo() {}
func bar() {}

func two_statements_per_line() {
    foo(); bar(); // Noncompliant
}

func single_statement_per_line(x int) {
    x = 42
}

func single_statement_per_line_with_semicolon(x int) {
    x = 42;
}
