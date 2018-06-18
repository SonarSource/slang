if (true) { // Noncompliant {{Remove this useless "if" statement.}}
//  ^^^^
  return 1
}

if (false) { // Noncompliant
  return 1
}

if (condition) {
  return 1
} else if (true) { // Noncompliant
  return 1
}

if (true) // Noncompliant
  return 1


x = if (true) 1 else 2 // Noncompliant
x = if (condition) 1 else 2