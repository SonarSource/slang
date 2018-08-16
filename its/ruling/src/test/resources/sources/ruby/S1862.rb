if param == 1
  openWindow()
elsif param == 2
  closeWindow()
elsif param == 1  # Noncompliant
  moveWindowToTheBackground()
end

case i
  when 1
    # ...
  when 3
    # ...
  when 1  # Noncompliant
    # ...
  else
    # ...
end

if param == 1
  openWindow()
elsif param == 2
  closeWindow()
elsif param == 3
  moveWindowToTheBackground()
end

case i
  when 1
    # ...
  when 3
    # ...
  else
    # ...
end
