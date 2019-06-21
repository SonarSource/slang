package samples

import "fmt"

func sample() {
	fmt.Print("hello")
	return // Noncompliant
	fmt.Print("world!")
}
