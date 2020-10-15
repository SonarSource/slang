params = "user=admin&password=Password123"   # Sensitive
sqlserver = "pgsql:host=localhost port=5432 dbname=test user=postgres password=postgres"   # Sensitive

password = "Password"                 # Compliant
password = "[id='password']"          # Compliant
password = "custom.password"          # Compliant
password = "trustStorePassword"       # Compliant
password = "connection.password"      # Compliant
password = "/users/resetUserPassword" # Compliant
