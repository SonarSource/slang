# COMMENT
def foo(args)
    p "hello " +
=begin comment
=end
            "world"
    a = 0

=begin
    multiline
    comment

=end
  a = a + 5 #comment
  return a
end

foo(1)
