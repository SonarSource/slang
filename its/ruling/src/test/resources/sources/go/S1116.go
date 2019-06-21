// S1116
package samples

import "fmt"

func foo() {

  for ; i < 10; i++ {
    ; // Noncompliant {{Remove this empty statement.}}
  }
  fmt.Println("doSomethingElse");;    // Noncompliant {{Remove this empty statement.}}
}
