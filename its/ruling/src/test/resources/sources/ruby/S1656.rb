class MyClass
  def foo()
    @name = @name
    @name = [1, 2, 3]

    @name[1] = @name[1]
    a = 1
    b = 2
    a, b = a, b
  end
end

A = 1
A = A
