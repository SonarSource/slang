package main

func foo() {
  pwd := "secret" // NOSONAR
  pwd = "secret"  // raise an issue S2068
}
