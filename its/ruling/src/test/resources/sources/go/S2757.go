package samples

func foo() int {
  var target, num = -5, 3

  target =- num  // Noncompliant
  target =+ num // Noncompliant

  return target
}
