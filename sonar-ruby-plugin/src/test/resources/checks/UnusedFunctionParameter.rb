def is_used_in_string_interpolation( a ) # Compliant, used below
  puts "Hello, #{a}"
end

def is_not_used( a ) # FN, string interpolation needs double quotes
  puts 'Hello, #{a}'
end

def is_not_used( a ) # Noncompliant
  puts "Hello"
end

def used_in_eval(a) # Compliant, used below
  eval 'a + 1'
end

class Foo

    def public_function(a) # Ignored
        puts "Hello"
    end

    private

    def is_used(a)
      puts "Hello, #{a}"
    end

    def is_not_used(a) # Noncompliant
      puts "Hello, World!"
    end
end

class Bar < Foo

  private def is_not_used(a) # compliant
    super # will call the override with the same parameters
  end
end

def nested_inside(not_used) # Noncompliant
    def nested_function(not_used_inside) # FN
    end
end
