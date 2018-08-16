
if b == 0  # Noncompliant
  doOneMoreThing()
else
  doOneMoreThing()
end

b = a > 12 ? 4 : 4;  # Noncompliant FN!!

case i  # Noncompliant
  when 1
    doSomething()
  when 2
    doSomething()
  when 3
    doSomething()
  else
    doSomething()
end


if b == 0
  doSomething()
elsif b == 1
  doSomething()
end
