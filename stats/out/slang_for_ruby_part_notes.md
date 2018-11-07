ruby|slang|notes
--|--|--
break|JumpTreeImpl(95%); NativeTreeImpl(5%);|`break` can be followed by statement (`break if cond`)
if|IfTreeImpl(77%); NativeTreeImpl(23%); BlockTreeImpl(0%);|`unless` and ternary
indexasgn|AssignmentExpressionTreeImpl(90%); NativeTreeImpl(10%);|In `a[1], b = 42`, `masgn` is top level and `a[1]` is `indexasgn` (so we map it to Native)
lvasgn|VariableDeclarationTreeImpl(73%); AssignmentExpressionTreeImpl(27%);|Declaration tree is created if it's a first assignment for this name
op_asgn|AssignmentExpressionTreeImpl(90%); NativeTreeImpl(10%);|`AssignmentExpressionTreeImpl` is created only for `+=`
resbody|CatchTreeImpl(52%); NativeTreeImpl(42%); BlockTreeImpl(6%);|???
send|NativeTreeImpl(97%); BinaryExpressionTreeImpl(2%); UnaryExpressionTreeImpl(1%); ThrowTreeImpl(0%);|Function call and variables are `send` 
str|StringLiteralTreeImpl(63%); NativeTreeImpl(37%);|`__FILE__` macro and dynamic strings (`<<-...`)
