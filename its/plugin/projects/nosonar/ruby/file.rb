def foo()
  
  if true # NOSONAR
  end
  
  if true # raise an issue S1145
  end
  
end
