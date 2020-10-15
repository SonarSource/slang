package samples

func noHardcodedCredentials() string  {
  password := "bar"
  return password
}

func hardcodedCredentials() {
  params := "user=admin&password=Password123" // Sensitive
  sqlserver := "pgsql:host=localhost port=5432 dbname=test user=postgres password=postgres" // Sensitive
}

func foo() {
  password := "Password" // Compliant
  password = "[id='password']" // Compliant
  password = "custom.password" // Compliant
  password = "trustStorePassword" // Compliant
  password = "connection.password" // Compliant
  password = "/users/resetUserPassword" // Compliant
}
