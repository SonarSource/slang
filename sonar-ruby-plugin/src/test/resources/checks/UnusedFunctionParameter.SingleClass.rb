class Foo

    def public_function(a)
        puts "Hello"
    end

    private

    def is_not_used(a) # FN, we don't see this method is private
      puts "Hello!"
    end
end
