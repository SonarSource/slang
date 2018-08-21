def foo()
  return 42 # Noncompliant
  puts "..."
end

def loop(a)
  while a < 5 do
    break # Noncompliant
    a += 1
  end
end
