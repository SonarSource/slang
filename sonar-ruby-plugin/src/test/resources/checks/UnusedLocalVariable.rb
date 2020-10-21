class Foo
    @@class_variable = 1

    def initialize(id)
      @instance_variable = id
    end

    def unused()
      x0 = 1 # Noncompliant

      x1 = 1 # FN, not used below
      puts "x1"

      x3 = 1 # FN, string interpolation works only with double quotes
      puts 'Hello #{x3}'

    end

    def unused_local_variable_same_name_as_global_variable()
      global_variable = 1 # Noncompliant
      puts "Hello #$global_variable"
    end

    def unused_local_variable_same_name_as_instance_variable()
      instance_variable = 1 # Noncompliant
      puts "Hello #@instance_variable"
    end

    def unused_local_variable_same_name_as_class_variable()
      class_variable = 1 # Noncompliant
      puts "Hello #@@class_variable"
    end

    def used_in_method_call()
      x = 1
      qix(x)
    end

    def used_in_eval()
      x = "foo" # Compliant, used below
      eval 'x + 2'

      with_underscore = 3
      eval 'with_underscore + 3'
    end

    def used_in_string_interpolation()
      x = 1 # Compliant, used below
      puts "Hello, #{x}"
    end

    def used_in_prepared_statement()
      placeholders = "" # Compliant, used below
      puts "WHERE location IN (#{placeholders})"

      id = "" # Compliant, used below
      puts "SELECT * FROM Cars WHERE Id = :id"
    end


    def qix(a)
        puts a
    end

    unused_outside_function = 1 # FN
end

$global_variable = 10

def nested_inside()
    not_used = 1 # Noncompliant

    def nested_function()
        nested_not_used = 2 # Noncompliant
    end
end
