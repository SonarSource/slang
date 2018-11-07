scala|slang|Notes
--|--|--
scala.meta.Case$CaseImpl|MatchCaseTreeImpl(55%); NativeTreeImpl(45%);| Pattern matching in anonymous function. (i.e. `List((1,2),(3,4)).map{case (x,y) => x + y}`). In addition, all case in a match statement with at least one conditional case (see Term$Param$TermParamImpl). 
scala.meta.Defn$Def$DefnDefImpl|FunctionDeclarationTreeImpl(90%); NativeTreeImpl(10%);| Function with many parameter clauses. `def f(i1: Int)(i2: Int) = ...`
scala.meta.Defn$Val$DefnValImpl|VariableDeclarationTreeImpl(64%); NativeTreeImpl(36%);| Variable and constant definition if there is some templating. `val  (x,y) = (1, 2) `
scala.meta.Defn$Var$DefnVarImpl|NativeTreeImpl(65%); VariableDeclarationTreeImpl(35%);| Same as DefnValImpl
scala.meta.Mod$Private$ModPrivateImpl|ModifierTreeImpl(80%); NativeTreeImpl(20%);| This is the correct expected result: in Scala, we can have "private[foo]" that means accessible inside foo package. This feature should not be mapped to a private modifier.
scala.meta.Term$ApplyInfix$TermApplyInfixImpl|NativeTreeImpl(65%); BinaryExpressionTreeImpl(35%);| Among the 65%, 99% comes from operator that are not supported, 1% from placholder (`(_ || _)`) and a minority from multiple argument on the right-hand side of an infix function application. (`foo ** (a,b)`)
scala.meta.Term$Assign$TermAssignImpl|NativeTreeImpl(80%); AssignmentExpressionTreeImpl(20%);| Assignment inside function call or object init are also TermAssignImpl but don't represent assignment expression in SLang sense. `class A(foo: Int); def f(foo : Int); f(foo = 2); new A(foo = 2)`
scala.meta.Term$Match$TermMatchImpl|MatchTreeImpl(88%); NativeTreeImpl(12%);| Match statement with at least one conditional case. `case "a" if(variable) => println("a")`
scala.meta.Term$Param$TermParamImpl|NativeTreeImpl(61%); ParameterTreeImpl(39%);| Function parameters with default value or modifier. 