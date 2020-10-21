x = "user=admin&password=Admin123"; # Noncompliant
user_password = "Admin123"; # Noncompliant

user_password = GetPassword();
password = "login=a&password=#{user_password}"; # Compliant
password = "login=a&password=#$global_password"; # Compliant
password = "login=a&password=#@instance_field"; # Compliant
password = "login=a&password=#@@class_field"; # Compliant
