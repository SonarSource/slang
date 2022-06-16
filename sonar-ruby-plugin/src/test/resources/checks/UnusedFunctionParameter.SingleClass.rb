class Foo

    def public_function(a)
        puts "Hello"
    end

    private

    def is_not_used(a) # Noncompliant
      puts "Hello!"
    end
end
