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

func databaseQuery() {
  query := "password=?"                 // Compliant
  query = "password=:password"          // Compliant
  query = "password=:param"             // Compliant
  query = "password='" + password + "'" // Compliant
  query = "password=%s"                 // Compliant
  query = "password=%v"                 // Compliant
}

func uriUserInfo() {
  url := "scheme://user:azerty123@domain.com" // Sensitive
  url = "scheme://user:@domain.com"           // Compliant
  url = "scheme://user@domain.com:80"         // Compliant
  url = "scheme://user@domain.com"            // Compliant
  url = "scheme://domain.com/user:azerty123"  // Compliant
}
