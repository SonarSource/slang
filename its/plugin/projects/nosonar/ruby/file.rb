def foo(args)

  pwd = "secret" # NOSONAR
  pwd = "secret" # raise an issue S2068

  return pwd
end
