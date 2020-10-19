// S1763
package samples

import "fmt"

func statement_after_return() {
    return // Noncompliant
    fmt.Print("Will not execute")
}

func return_after_return() {
    return // Noncompliant
    return
}

func statement_after_multi_values_return() (int, string) {
    return 42, "42" // Noncompliant
    fmt.Print("Will not execute")
}

func label_after_return(x int) int {
    if (x > 10) {
        goto Foo
    }
    return 42 // Compliant
Foo:
    return 21
}

func invalid_statement_after_return_in_label(x int) int {
    if (x > 10) {
        goto Foo
    }
    return 42
Foo:
    return 21 // Compliant - FN: labels are not properly supported
    fmt.Print("Will not execute")
}

func invalid_statement_after_return_in_label_block(x int) int {
    if (x > 10) {
        goto Foo
    }
    return 42
Foo:
    // Multiple statements after a label are analysed correctly only if they are inside a block "{ }"
    {
        return 21 // Noncompliant
        fmt.Print("Will not execute")
    }
}

func return_simple(){
  return // Compliant
}

func return_with_semicolon() int{
  return 0; // Compliant
}

func return_with_semicolon_and_empty_statement() int{
  return 0;; // Noncompliant
}

func return_followed_by_return() int{
  return 0; // Noncompliant
  return 0;
}

func return_semicolon_label() int{
  return 0; // Compliant
Foo:
  return 1;
}
