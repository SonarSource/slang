package main

import (
	"go/ast"
	"go/token"
	"strconv"
)

func (t *SlangMapper) mapReturnStmtImpl(stmt *ast.ReturnStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})
	returnToken := t.createTokenFromPosAstToken(stmt.Return, token.RETURN, "Return")
	slangField["keyword"] = returnToken.Token.TextRange
	children = t.appendNode(children, returnToken)

	if len(stmt.Results) == 0 {
		slangField["body"] = nil
	} else if len(stmt.Results) == 1 {
		body := t.mapExpr(stmt.Results[0], "["+strconv.Itoa(0)+"]")
		slangField["body"] = body
		children = t.appendNode(children, body)
	} else {
		//Slang does not support multiple body, map the whole node to native
		for i := 0; i < len(stmt.Results); i++ {
			children = t.appendNode(children, t.mapExpr(stmt.Results[i], "["+strconv.Itoa(i)+"]"))
		}
		return t.createNativeNode(stmt, children, fieldName+"(ReturnStmt)")
	}

	return t.createNode(stmt, children, fieldName+"(ReturnStmt)", "Return", slangField)
}

func (t *SlangMapper) mapIdentImpl(ident *ast.Ident, fieldName string) *Node {
	slangField := make(map[string]interface{})
	var slangType string
	var children []*Node

	switch ident.Name {
	case "true", "false", "nil":
		slangType = "Literal"
		slangField["value"] = ident.Name
	case "_":
		slangType = "PlaceHolder"
		placeHolderToken := t.createExpectedToken(ident.NamePos, "_", "PlaceHolder", "KEYWORD")
		children = t.appendNode(children, placeHolderToken)
		slangField["placeHolderToken"] = placeHolderToken.TextRange
	default:
		slangType = "Identifier"
		slangField["name"] = ident.Name
	}

	return t.createNode(ident, children, fieldName+"(Ident)", slangType, slangField)
}

func (t *SlangMapper) mapFileImpl(file *ast.File, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})
	var declarations []*Node

	packageDecl := t.mapPackageDecl(file)
	children = t.appendNode(children, packageDecl)
	declarations = append(declarations, packageDecl)

	var nodeListDecls []*Node
	for i := 0; i < len(file.Decls); i++ {
		nodeListDecls = t.appendNode(nodeListDecls, t.mapDecl(file.Decls[i], "["+strconv.Itoa(i)+"]"))
	}
	children = t.appendNodeList(children, nodeListDecls, "Decls([]Decl)")
	declarations = append(declarations, t.filterOutComments(nodeListDecls)...)

	slangField["declarations"] = declarations
	slangField["firstCpdToken"] = nil
	return t.createNode(file, children, fieldName, "TopLevel", slangField)
}

func (t *SlangMapper) mapDeclImpl(decl ast.Decl, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBadDeclImpl(decl *ast.BadDecl, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFuncDeclImpl(decl *ast.FuncDecl, fieldName string) *Node {
	var children []*Node
	var nativeChildren []*Node
	slangField := make(map[string]interface{})

	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Type.Func, token.FUNC, "Type.Func"))

	receiver := t.mapFieldListParams(decl.Recv, "Recv")
	children = t.appendNode(children, receiver)

	funcName := t.mapIdent(decl.Name, "Name")
	children = t.appendNode(children, funcName)
	slangField["name"] = funcName

	parameters := t.mapFieldListParams(decl.Type.Params, "Params")
	children = t.appendNode(children, parameters)
	formalParameters := t.getFormalParameter(parameters)
	if receiver != nil {
		formalParameters = append([]*Node{receiver}, formalParameters...)
	}
	slangField["formalParameters"] = formalParameters

	funcResults := t.mapFieldListResults(decl.Type.Results, "Results")
	children = t.appendNode(children, funcResults)
	slangField["returnType"] = funcResults

	funcBody := t.mapBlockStmt(decl.Body, "Body")
	children = t.appendNode(children, funcBody)
	slangField["body"] = funcBody

	//Required by SLang; Go does not have constructors
	slangField["isConstructor"] = false
	//Go does not have explicit modifiers
	slangField["modifiers"] = nil
	//Other children of the function node
	slangField["nativeChildren"] = nativeChildren

	return t.createNode(decl, children, fieldName+"(FuncDecl)", "FunctionDeclaration", slangField)
}

func (t *SlangMapper) mapFuncLitImpl(lit *ast.FuncLit, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	children = t.appendNode(children, t.createTokenFromPosAstToken(lit.Type.Func, token.FUNC, "Type.Func"))

	parameters := t.mapFieldListParams(lit.Type.Params, "Params")
	children = t.appendNode(children, parameters)
	slangField["formalParameters"] = t.getFormalParameter(parameters)

	funcResults := t.mapFieldListResults(lit.Type.Results, "Results")
	children = t.appendNode(children, funcResults)
	slangField["returnType"] = funcResults

	funcBody := t.mapBlockStmt(lit.Body, "Body")
	children = t.appendNode(children, funcBody)
	slangField["body"] = funcBody

	//Required by SLang; Go does not have constructors
	slangField["isConstructor"] = false
	//Go does not have explicit modifiers
	slangField["modifiers"] = nil
	//Other children of the function node
	slangField["nativeChildren"] = nil

	return t.createNode(lit, children, fieldName+"(FuncLit)", "FunctionDeclaration", slangField)
}

func (t *SlangMapper) getFormalParameter(node *Node) []*Node {
	var formalParameters []*Node
	//Get all FieldListParams lists
	childrenWithoutComment := t.filterOutComments(node.Children)
	for i := 1; i < len(childrenWithoutComment)-1; i++ {
		//Get all params inside this list (excluding comma)
		currentList := t.filterOutComments(childrenWithoutComment[i].Children)
		for j := 0; j < len(currentList); j = j + 2 {
			formalParameters = append(formalParameters, currentList[j])
		}
	}
	return formalParameters
}

func (t *SlangMapper) mapGenDeclImpl(decl *ast.GenDecl, fieldName string) *Node {
	slangField := make(map[string]interface{})

	switch decl.Tok {
	case token.CONST:
		slangField["isVal"] = true
	case token.VAR:
		slangField["isVal"] = false
	default:
		// token.IMPORT, token.TYPE, mapped to native
		return nil
	}

	if len(decl.Specs) != 1 {
		//The node can not be mapped to a typed Slang node, create a native node
		return nil
	}

	valueSpec, ok := decl.Specs[0].(*ast.ValueSpec)
	if !ok || len(valueSpec.Names) != 1 || valueSpec.Names[0].Name == "_" {
		// The spec of this declaration is not a valueSpec, or have multiple identifier (i, j := 1, 2)
		// or is a placeholder, we map it to native
		return nil
	}

	var children []*Node
	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.TokPos, decl.Tok, "Tok"))
	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Lparen, token.LPAREN, "Lparen"))

	identifier := t.mapIdent(valueSpec.Names[0], "[0]")
	children = t.appendNode(children, identifier)
	slangField["identifier"] = identifier

	typ := t.mapExpr(valueSpec.Type, "Type")
	children = t.appendNode(children, typ)
	slangField["type"] = typ

	var initializer *Node
	nValues := len(valueSpec.Values)

	if nValues > 1 {
		var nodeListValues []*Node
		for i := 0; i < len(valueSpec.Values); i++ {
			nodeListValues = t.appendNode(nodeListValues, t.mapExpr(valueSpec.Values[i], "["+strconv.Itoa(i)+"]"))
		}
		//Wrap all values in a native node
		initializer = t.createNativeNodeWithChildren(nodeListValues, "Values([]Expr)")
	} else if nValues == 1 {
		initializer = t.mapExpr(valueSpec.Values[0], "[0]")
	} else {
		initializer = nil
	}

	children = t.appendNode(children, initializer)
	slangField["initializer"] = initializer

	children = t.appendNode(children, t.createTokenFromPosAstToken(decl.Rparen, token.RPAREN, "Rparen"))

	return t.createNode(decl, children, fieldName+"(GenDecl)", "VariableDeclaration", slangField)
}

func (t *SlangMapper) mapFieldListParamsImpl(list *ast.FieldList, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldListResultsImpl(list *ast.FieldList, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldListBraceImpl(list *ast.FieldList, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFuncTypeImpl(funcType *ast.FuncType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFuncTypeDeclImpl(funcType *ast.FuncType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBlockStmtImpl(blockStmt *ast.BlockStmt, fieldName string) *Node {
	var children []*Node
	children = t.appendNode(children, t.createTokenFromPosAstToken(blockStmt.Lbrace, token.LBRACE, "Lbrace"))
	for i := 0; i < len(blockStmt.List); i++ {
		children = t.appendNode(children, t.mapStmt(blockStmt.List[i], "["+strconv.Itoa(i)+"]"))
	}
	children = t.appendNode(children, t.createTokenFromPosAstToken(blockStmt.Rbrace, token.RBRACE, "Rbrace"))

	slangField := make(map[string]interface{})

	// children without the braces
	slangField["statementOrExpressions"] = t.filterOutComments(children[1 : len(children)-1])

	return t.createNode(blockStmt, children, fieldName+"(BlockStmt)", "Block", slangField)
}

func (t *SlangMapper) mapSpecImpl(spec ast.Spec, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldImpl(field *ast.Field, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldResultImpl(field *ast.Field, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapFieldParamImpl(field *ast.Field, fieldName string) *Node {
	var children []*Node

	nNames := len(field.Names)

	if nNames <= 0 {
		return nil
	}
	//Go paramter can share the type with multiple identifier ex: f(a, b int)
	//We will create a parameter node without type for the firsts and with type for the last
	for i := 0; i < nNames-1; i++ {
		paramterIdent := t.mapIdent(field.Names[i], fieldName+"["+strconv.Itoa(i)+"]")
		parameter := t.createParameter(field.Names[i], paramterIdent, nil, fieldName)
		children = t.appendNode(children, parameter)
	}
	lastParameterIdent := t.mapIdent(field.Names[nNames-1], fieldName+"["+strconv.Itoa(nNames-1)+"]")
	lastParameterType := t.mapExpr(field.Type, "Type")

	lastParameter := t.createParameter(field.Names[nNames-1], lastParameterIdent, lastParameterType, fieldName)
	children = t.appendNode(children, lastParameter)
	children = t.appendNode(children, t.mapBasicLit(field.Tag, "Tag"))

	return t.createNativeNode(field, children, fieldName+"(Field)")
}

func (t *SlangMapper) createParameter(ident *ast.Ident, parameterIdent, typ *Node, fieldName string) *Node {
	slangField := make(map[string]interface{})
	children := []*Node{parameterIdent}
	if typ != nil {
		children = t.appendNode(children, typ)
	}
	if ident.Name == "_" {
		//Placeholder parameters are mapped to native
		return t.createNativeNode(ident, children, "Parameter")
	} else {
		slangField["identifier"] = parameterIdent
		slangField["type"] = typ
		slangField["modifiers"] = nil    //No paramter modifier in Go
		slangField["defaultValue"] = nil //No default value in Go
		return t.createNode(ident, children, fieldName+"(Parameter)", "Parameter", slangField)
	}
}

func (t *SlangMapper) mapStmtImpl(stmt ast.Stmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapImportSpecImpl(spec *ast.ImportSpec, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapTypeSpecImpl(spec *ast.TypeSpec, fieldName string) *Node {
	slangField := make(map[string]interface{})
	var children []*Node

	specName := t.mapIdent(spec.Name, "Name")
	children = t.appendNode(children, specName)
	slangField["identifier"] = specName.TextRange

	children = t.appendNode(children, t.createTokenFromPosAstToken(spec.Assign, token.ASSIGN, "Assign"))
	children = t.appendNode(children, t.mapExpr(spec.Type, "Type"))

	//ClassTree in SLang contains everything (including identifier), we create a new node for this purpose
	classTree := t.createNativeNode(spec, children, fieldName+"(TypeSpecWrapped)")
	slangField["classTree"] = classTree
	return t.createNode(spec, []*Node{classTree}, fieldName+"(TypeSpec)", "ClassDeclaration", slangField)
}

func (t *SlangMapper) mapValueSpecImpl(spec *ast.ValueSpec, fieldName string) *Node {
	// ValueSpec represents declaration, but they will be mapped inside mapGenDeclImpl to know if the node is a const or not.
	return nil
}

func (t *SlangMapper) mapExprImpl(expr ast.Expr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapAssignStmtImpl(stmt *ast.AssignStmt, fieldName string) *Node {
	if stmt.Lhs == nil || stmt.Rhs == nil {
		//SLang does not support null LHS or RHS for assignment
		return nil
	}

	var operator string
	var isVarDecl = false
	switch stmt.Tok {
	case token.ASSIGN:
		operator = "EQUAL"
	case token.ADD_ASSIGN:
		operator = "PLUS_EQUAL"
	case token.DEFINE:
		if len(stmt.Lhs) != 1 {
			//i, j := 1, 2; SLang does not support this, map to Native
			return nil
		}
		isVarDecl = true
	default:
		// Slang only support = and +=, other compound assignments are ignored.
		return nil
	}

	var leftHandSide *Node
	if len(stmt.Lhs) > 1 {
		var nodeListLhs []*Node
		for i := 0; i < len(stmt.Lhs); i++ {
			nodeListLhs = t.appendNode(nodeListLhs, t.mapExpr(stmt.Lhs[i], "["+strconv.Itoa(i)+"]"))
		}
		leftHandSide = t.createNativeNodeWithChildren(nodeListLhs, "Lhs([]Expr)")
	} else {
		leftHandSide = t.mapExpr(stmt.Lhs[0], "[0]")
	}

	var children []*Node
	children = t.appendNode(children, leftHandSide)

	children = t.appendNode(children, t.createTokenFromPosAstToken(stmt.TokPos, stmt.Tok, "Tok"))

	var rightHandSide *Node
	if len(stmt.Rhs) > 1 {
		var nodeListRhs []*Node
		for i := 0; i < len(stmt.Rhs); i++ {
			nodeListRhs = t.appendNode(nodeListRhs, t.mapExpr(stmt.Rhs[i], "["+strconv.Itoa(i)+"]"))
		}
		rightHandSide = t.createNativeNodeWithChildren(nodeListRhs, "Rhs([]Expr)")
	} else {
		rightHandSide = t.mapExpr(stmt.Rhs[0], "[0]")
	}
	children = t.appendNode(children, rightHandSide)

	slangField := make(map[string]interface{})
	if isVarDecl {
		if leftHandSide.SlangType != "Identifier" {
			// identifier should be an IdentifierTree, if it not the case (Placeholder, ...), we create a native node
			return t.createNativeNode(stmt, children, fieldName+"(AssignDefineStmt)")
		}
		slangField["identifier"] = leftHandSide
		slangField["type"] = nil
		slangField["initializer"] = rightHandSide
		slangField["isVal"] = false
		return t.createNode(stmt, children, fieldName+"(AssignDefineStmt)", "VariableDeclaration", slangField)
	} else {
		slangField["operator"] = operator
		slangField["leftHandSide"] = leftHandSide
		slangField["statementOrExpression"] = rightHandSide
		return t.createNode(stmt, children, fieldName+"(AssignStmt)", "AssignmentExpression", slangField)
	}
}

func (t *SlangMapper) mapBadStmtImpl(stmt *ast.BadStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBranchStmtImpl(stmt *ast.BranchStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	var jumpKind string

	switch stmt.Tok {
	case token.BREAK:
		jumpKind = "BREAK"
	case token.CONTINUE:
		jumpKind = "CONTINUE"
	default:
		return nil
	}

	branchToken := t.createTokenFromPosAstToken(stmt.TokPos, stmt.Tok, "Tok"+jumpKind)
	children = t.appendNode(children, branchToken)
	slangField["keyword"] = branchToken.TextRange
	slangField["kind"] = jumpKind

	label := t.mapIdent(stmt.Label, "Label")
	children = t.appendNode(children, label)
	slangField["label"] = label

	return t.createNode(stmt, children, fieldName+"(BranchStmt)", "Jump", slangField)
}

func (t *SlangMapper) mapCaseClauseImpl(clause *ast.CaseClause, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	children = t.handleSwitchCase(clause.Case, len(clause.List) == 0, children)

	var clauseList []*Node
	for i := 0; i < len(clause.List); i++ {
		clauseList = t.appendNode(clauseList, t.mapExpr(clause.List[i], "["+strconv.Itoa(i)+"]"))
	}
	//SLang requires a tree as expression and not a list, we wrap it in a native node
	caseExpression := t.createNativeNodeWithChildren(clauseList, "CaseExprList")
	children = t.appendNode(children, caseExpression)
	slangField["expression"] = caseExpression

	children = t.appendNode(children, t.createTokenFromPosAstToken(clause.Colon, token.COLON, "Colon"))

	var nodeListBody []*Node
	for i := 0; i < len(clause.Body); i++ {
		nodeListBody = t.appendNode(nodeListBody, t.mapStmt(clause.Body[i], "["+strconv.Itoa(i)+"]"))
	}

	//SLang requires a tree as body and not a list, we wrap it in a block
	nodeListBodyWithoutComment := t.filterOutComments(nodeListBody)
	var caseBody *Node

	if len(nodeListBodyWithoutComment) == 1 && nodeListBodyWithoutComment[0].SlangType == "Block" {
		caseBody = nodeListBodyWithoutComment[0]
	} else {
		slangFieldBlock := make(map[string]interface{})
		slangFieldBlock["statementOrExpressions"] = nodeListBodyWithoutComment
		caseBody = t.createNode(nil, nodeListBody, fieldName+"(BlockStmt)", "Block", slangFieldBlock)
	}

	children = t.appendNode(children, caseBody)
	slangField["body"] = caseBody

	return t.createNode(clause, children, fieldName+"(CaseClause)", "MatchCase", slangField)
}

func (t *SlangMapper) mapCommClauseImpl(clause *ast.CommClause, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapDeclStmtImpl(stmt *ast.DeclStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapDeferStmtImpl(stmt *ast.DeferStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapEmptyStmtImpl(stmt *ast.EmptyStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapExprStmtImpl(stmt *ast.ExprStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapForStmtImpl(stmt *ast.ForStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	hasInitOrPost := stmt.Init != nil || stmt.Post != nil

	if stmt.Cond == nil && !hasInitOrPost {
		//For loop with null header, not possible currently in SLang, we let the caller map it to Native
		return nil
	}

	forToken := t.createTokenFromPosAstToken(stmt.For, token.FOR, "For")
	children = t.appendNode(children, forToken)
	slangField["keyword"] = forToken.TextRange

	var condition *Node
	var kind string

	if !hasInitOrPost {
		condition = t.mapExpr(stmt.Cond, "Cond")
		children = t.appendNode(children, condition)
		kind = "WHILE"
	} else {
		var forHeaderList []*Node
		forHeaderList = t.appendNode(forHeaderList, t.mapStmt(stmt.Init, "Init"))
		forHeaderList = t.appendNode(forHeaderList, t.mapExpr(stmt.Cond, "Cond"))
		forHeaderList = t.appendNode(forHeaderList, t.mapStmt(stmt.Post, "Post"))

		//Wrap the 3 elements of the for loop header into one single node
		condition = t.createNativeNodeWithChildren(forHeaderList, "ForHeader")
		children = t.appendNode(children, condition)
		kind = "FOR"
	}
	slangField["condition"] = condition
	slangField["kind"] = kind

	body := t.mapBlockStmt(stmt.Body, "Body")
	children = t.appendNode(children, body)
	slangField["body"] = body

	return t.createNode(stmt, children, fieldName+"(ForStmt)", "Loop", slangField)
}

func (t *SlangMapper) mapGoStmtImpl(stmt *ast.GoStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapIfStmtImpl(ifStmt *ast.IfStmt, fieldName string) *Node {
	var children []*Node
	ifToken := t.createTokenFromPosAstToken(ifStmt.If, token.IF, "If")
	children = t.appendNode(children, ifToken)

	var condition *Node
	if ifStmt.Init != nil {
		condition = t.createAdditionalInitAndCond(ifStmt.Init, ifStmt.Cond)
	} else {
		condition = t.mapExpr(ifStmt.Cond, "Cond")
	}

	children = t.appendNode(children, condition)

	thenBranch := t.mapBlockStmt(ifStmt.Body, "Body")
	children = t.appendNode(children, thenBranch)

	elseBranch := t.mapStmt(ifStmt.Else, "Else")
	children = t.appendNode(children, elseBranch)

	slangField := make(map[string]interface{})

	slangField["ifKeyword"] = ifToken.TextRange
	slangField["condition"] = condition
	slangField["thenBranch"] = thenBranch
	slangField["elseKeyword"] = nil
	slangField["elseBranch"] = elseBranch

	childrenWithoutComments := t.filterOutComments(children)
	if elseBranch != nil {
		for i := len(childrenWithoutComments) - 1; i >= 0; i-- {
			if childrenWithoutComments[i] == elseBranch {
				// else keyword is necessarily before, and has been added to the children when calling "appendNode"
				slangField["elseKeyword"] = childrenWithoutComments[i-1].TextRange
				break
			}
		}
	}

	return t.createNode(ifStmt, children, fieldName+"(IfStmt)", "If", slangField)
}

func (t *SlangMapper) mapIncDecStmtImpl(stmt *ast.IncDecStmt, fieldName string) *Node {
	var operatorName = "DECREMENT"
	if token.INC == stmt.Tok {
		operatorName = "INCREMENT"
	}

	var children []*Node
	slangField := make(map[string]interface{})

	operand := t.mapExpr(stmt.X, "X")
	children = t.appendNode(children, operand)
	slangField["operand"] = operand

	operator := t.createTokenFromPosAstToken(stmt.TokPos, stmt.Tok, "Tok")
	children = t.appendNode(children, operator)
	slangField["operator"] = operatorName

	return t.createNode(stmt, children, fieldName+"(UnaryExpression)", "UnaryExpression", slangField)
}

func (t *SlangMapper) mapLabeledStmtImpl(stmt *ast.LabeledStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapRangeStmtImpl(stmt *ast.RangeStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	forToken := t.createTokenFromPosAstToken(stmt.For, token.FOR, "For")
	children = t.appendNode(children, forToken)
	slangField["keyword"] = forToken.TextRange

	var rangeHeaderList []*Node

	rangeHeaderList = t.appendNode(rangeHeaderList, t.mapExpr(stmt.Key, "Key"))
	rangeHeaderList = t.appendNode(rangeHeaderList, t.mapExpr(stmt.Value, "Value"))
	rangeHeaderList = t.appendNode(rangeHeaderList, t.createTokenFromPosAstToken(stmt.TokPos, stmt.Tok, "Tok"))
	rangeHeaderList = t.appendNode(rangeHeaderList, t.mapExpr(stmt.X, "X"))

	//Wrap all element of the range loop into one single node
	condition := t.createNativeNodeWithChildren(rangeHeaderList, "RangeHeader")
	children = t.appendNode(children, condition)
	slangField["condition"] = condition

	body := t.mapBlockStmt(stmt.Body, "Body")
	children = t.appendNode(children, body)
	slangField["body"] = body

	slangField["kind"] = "FOR"

	return t.createNode(stmt, children, fieldName+"(RangeStmt)", "Loop", slangField)
}

func (t *SlangMapper) mapSelectStmtImpl(stmt *ast.SelectStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapSendStmtImpl(stmt *ast.SendStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapSwitchStmtImpl(stmt *ast.SwitchStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	keywordToken := t.createTokenFromPosAstToken(stmt.Switch, token.SWITCH, "Switch")
	children = t.appendNode(children, keywordToken)
	slangField["keyword"] = keywordToken.TextRange

	var expressionList []*Node
	expressionList = t.appendNode(expressionList, t.mapStmt(stmt.Init, "Init"))
	expressionList = t.appendNode(expressionList, t.mapExpr(stmt.Tag, "Tag"))

	//Wrap the tag and init into one native node
	expression := t.createNativeNodeWithChildren(expressionList, "InitAndTag")
	children = t.appendNode(children, expression)
	slangField["expression"] = expression

	body := t.mapBlockStmt(stmt.Body, "Body")
	children = t.appendNode(children, body)
	slangField["cases"] = t.getMatchCases(body)

	return t.createNode(stmt, children, fieldName+"(SwitchStmt)", "Match", slangField)
}

func (t *SlangMapper) mapTypeSwitchStmtImpl(stmt *ast.TypeSwitchStmt, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	keywordToken := t.createTokenFromPosAstToken(stmt.Switch, token.SWITCH, "Switch")
	children = t.appendNode(children, keywordToken)
	slangField["keyword"] = keywordToken.TextRange

	var expressionList []*Node
	expressionList = t.appendNode(expressionList, t.mapStmt(stmt.Init, "Init"))
	expressionList = t.appendNode(expressionList, t.mapStmt(stmt.Assign, "Assign"))

	//Wrap the init and Assign into one native node
	expression := t.createNativeNodeWithChildren(expressionList, "InitAndAssign")
	children = t.appendNode(children, expression)
	slangField["expression"] = expression

	body := t.mapBlockStmt(stmt.Body, "Body")
	children = t.appendNode(children, body)
	slangField["cases"] = t.getMatchCases(body)

	return t.createNode(stmt, children, fieldName+"(TypeSwitchStmt)", "Match", slangField)
}

func (t *SlangMapper) getMatchCases(node *Node) []*Node {
	bodyWithoutComment := t.filterOutComments(node.Children)
	return bodyWithoutComment[1 : len(bodyWithoutComment)-1]
}

func (t *SlangMapper) mapArrayTypeImpl(arrayType *ast.ArrayType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBadExprImpl(expr *ast.BadExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBinaryExprImpl(expr *ast.BinaryExpr, fieldName string) *Node {

	var operatorName = ""
	switch expr.Op {
	case token.ADD:
		operatorName = "PLUS"
	case token.SUB:
		operatorName = "MINUS"
	case token.MUL:
		operatorName = "TIMES"
	case token.QUO:
		operatorName = "DIVIDED_BY"
	case token.EQL:
		operatorName = "EQUAL_TO"
	case token.NEQ:
		operatorName = "NOT_EQUAL_TO"
	case token.GTR:
		operatorName = "GREATER_THAN"
	case token.GEQ:
		operatorName = "GREATER_THAN_OR_EQUAL_TO"
	case token.LSS:
		operatorName = "LESS_THAN"
	case token.LEQ:
		operatorName = "LESS_THAN_OR_EQUAL_TO"
	case token.LAND:
		operatorName = "CONDITIONAL_AND"
	case token.LOR:
		operatorName = "CONDITIONAL_OR"
	default:
		// all the other binary operators are not mapped
		return nil

	}

	var children []*Node
	slangField := make(map[string]interface{})

	leftOperand := t.mapExpr(expr.X, "operand")
	children = t.appendNode(children, leftOperand)
	slangField["leftOperand"] = leftOperand

	operator := t.createTokenFromPosAstToken(expr.OpPos, expr.Op, "Op")
	children = t.appendNode(children, operator)
	slangField["operator"] = operatorName
	slangField["operatorToken"] = operator.TextRange

	rightOperand := t.mapExpr(expr.Y, "operand")
	children = t.appendNode(children, rightOperand)
	slangField["rightOperand"] = rightOperand

	return t.createNode(expr, children, fieldName+"(BinaryExpr)", "BinaryExpression", slangField)
}

func (t *SlangMapper) mapCallExprImpl(expr *ast.CallExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapChanTypeImpl(chanType *ast.ChanType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapCompositeLitImpl(lit *ast.CompositeLit, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapEllipsisImpl(ellipsis *ast.Ellipsis, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapIndexExprImpl(expr *ast.IndexExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapInterfaceTypeImpl(interfaceType *ast.InterfaceType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapKeyValueExprImpl(expr *ast.KeyValueExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapMapTypeImpl(mapType *ast.MapType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapParenExprImpl(expr *ast.ParenExpr, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	leftParen := t.createTokenFromPosAstToken(expr.Lparen, token.LPAREN, "Lparen")
	slangField["leftParenthesis"] = leftParen.TextRange
	children = t.appendNode(children, leftParen)

	nestedExpr := t.mapExpr(expr.X, "X")
	slangField["expression"] = nestedExpr
	children = t.appendNode(children, nestedExpr)

	rightParen := t.createTokenFromPosAstToken(expr.Rparen, token.RPAREN, "Rparen")
	children = t.appendNode(children, rightParen)
	slangField["rightParenthesis"] = rightParen.TextRange

	return t.createNode(expr, children, fieldName+"(ParenExpr)", "ParenthesizedExpression", slangField)
}

func (t *SlangMapper) mapSelectorExprImpl(expr *ast.SelectorExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapSliceExprImpl(expr *ast.SliceExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapStarExprImpl(expr *ast.StarExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapStructTypeImpl(structType *ast.StructType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapTypeAssertExprImpl(expr *ast.TypeAssertExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapUnaryExprImpl(expr *ast.UnaryExpr, fieldName string) *Node {
	var operatorName = ""
	switch expr.Op {
	case token.ADD:
		operatorName = "PLUS"
	case token.SUB:
		operatorName = "MINUS"
	case token.NOT:
		operatorName = "NEGATE"
	default:
		// only covering unary operators which are supported by SLang
		return nil
	}

	var children []*Node
	slangField := make(map[string]interface{})

	operator := t.createTokenFromPosAstToken(expr.OpPos, expr.Op, "Op")
	children = t.appendNode(children, operator)
	slangField["operator"] = operatorName

	operand := t.mapExpr(expr.X, "X")
	children = t.appendNode(children, operand)
	slangField["operand"] = operand

	return t.createNode(expr, children, fieldName+"(UnaryExpression)", "UnaryExpression", slangField)
}
