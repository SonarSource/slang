params = "user=admin&password=Password123"   # Sensitive
sqlserver = "pgsql:host=localhost port=5432 dbname=test user=postgres password=postgres"   # Sensitive

password = "Password"                 # Compliant
password = "[id='password']"          # Compliant
password = "custom.password"          # Compliant
password = "trustStorePassword"       # Compliant
password = "connection.password"      # Compliant
password = "/users/resetUserPassword" # Compliant

# database queries
query1 = "password=?"                  # Compliant
query2 = "password=:password"          # Compliant
query3 = "password=:param"             # Compliant
query4 = "password='" + password + "'" # Compliant
query5 = "password=%s"                 # Compliant
query6 = "password=%v"                 # Compliant

# URI user info
url1 = "scheme://user:azerty123@domain.com"  # Sensitive
url2 = "scheme://user:@domain.com"           # Compliant
url3 = "scheme://user@domain.com:80"         # Compliant
url4 = "scheme://user@domain.com"            # Compliant
url5 = "scheme://domain.com/user:azerty123"  # Compliant
