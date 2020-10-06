package main

func foo(name string) {
  if true {  // NOSONAR
  }
  if true {  // raise an issue S1145
  }
}
