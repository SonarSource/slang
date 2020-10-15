package sources.kotlin

class S2068 {

    fun foo() {
        val params = "user=admin&password=Password123"   // Sensitive
        val sqlserver = "pgsql:host=localhost port=5432 dbname=test user=postgres password=postgres"   // Sensitive
    }

    fun f1() {
        val password = "Password"                 // Compliant
    }
    fun f2() {
        val password = "[id='password']"          // Compliant
    }
    fun f3() {
        val password = "custom.password"          // Compliant
    }
    fun f4() {
        val password = "trustStorePassword"       // Compliant
    }
    fun f5() {
        val password = "connection.password"      // Compliant
    }
    fun f6() {
        val password = "/users/resetUserPassword" // Compliant
    }
}
