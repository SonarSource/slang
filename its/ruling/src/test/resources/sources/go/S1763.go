// S1763
package samples

import "fmt"

func sample() {
	fmt.Print("hello")
	return // Noncompliant
	fmt.Print("world!")
}


func sample2() {
	//Continue and break behave as in Java
	sum := 0
OUTER:
	for i := 1; i < 500; i++ {
		if i%2 != 0 { // skip odd numbers
			continue  // Noncompliant
			fmt.Print("Dead code1!")
		}
		if i == 100 { // stop at 100
			break  // Noncompliant
			fmt.Print("Dead code2!")
		}
		if i%2 != 0 { // skip odd numbers
			continue OUTER  // Noncompliant
			fmt.Print("Dead code3!")
		} else {
		    fmt.Print("Not dead code!")  // Compliant
		}
		sum += i
	}
}

func fn(x int) int {
	if (x > 10) {
		goto Foo
	}
	return 42 // Compliant
FOO:
	return 21
}
