kotlin|slang|notes
--|--|--
KtBinaryExpression|BinaryExpressionTreeImpl(56%); NativeTreeImpl(28%); AssignmentExpressionTreeImpl(16%);|Mapped to Native when 0 or 1 argument (when is it possible?)
KtConstantExpression|IntegerLiteralTreeImpl(55%); LiteralTreeImpl(45%);|LiteralTreeImpl is created i.e. for `true`
KtDynamicType|NativeTreeImpl(94%); IdentifierTreeImpl(6%);|IdentifierTreeImpl is created when function return type, should be changed? `fun foo():dynamic ...`
KtNamedFunction|FunctionDeclarationTreeImpl(88%); NativeTreeImpl(12%);|Native is created for extension functions `fuc A.fun()...`
KtNullableType|NativeTreeImpl(79%); IdentifierTreeImpl(21%);|IdentifierTreeImpl is created when function return type, should be changed? `fun foo(): Int? ...`
KtParameter|ParameterTreeImpl(91%); NativeTreeImpl(9%);|Native is created for parameter with initializer `fun foo(a:Int = 1)...`
KtPostfixExpression|NativeTreeImpl(79%); UnaryExpressionTreeImpl(21%);|???
KtProperty|VariableDeclarationTreeImpl(68%); NativeTreeImpl(32%);|Native is created for class properties
KtStringTemplateExpression|StringLiteralTreeImpl(87%); NativeTreeImpl(13%);|Native is created for strings with interpolation
KtUserType|NativeTreeImpl(90%); IdentifierTreeImpl(10%);|IdentifierTreeImpl is created when function return type, should be changed? `fun foo(): Foo<A,B> ...`
KtWhenExpression|MatchTreeImpl(75%); NativeTreeImpl(25%);|Native is created when expression is missing `when {cond1 ->...}`
