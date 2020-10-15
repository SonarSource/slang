object Code {
  def foo() = {
    val params = "user=admin&password=Password123"   // Sensitive
    val sqlserver = "pgsql:host=localhost port=5432 dbname=test user=postgres password=postgres"   // Sensitive
  }

  def f1 = {
    val password = "Password" // Compliant
  }

  def f2 = {
    val password = "[id='password']" // Compliant
  }

  def f3 = {
    val password = "custom.password" // Compliant
  }

  def f4 = {
    val password = "trustStorePassword" // Compliant
  }

  def f5 = {
    val password = "connection.password" // Compliant
  }

  def f6 = {
    val password = "/users/resetUserPassword" // Compliant
  }
}
