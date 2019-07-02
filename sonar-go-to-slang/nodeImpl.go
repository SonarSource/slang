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

	switch ident.Name {
	case "true", "false", "nil":
		slangType = "Literal"
		slangField["value"] = ident.Name
	default:
		slangType = "Identifier"
		slangField["name"] = ident.Name
	}
	var children []*Node
	return t.createNode(ident, children, fieldName+"(Ident)", slangType, slangField)
}

func (t *SlangMapper) mapFileImpl(file *ast.File, fieldName string) *Node {
	var children []*Node
	slangField := make(map[string]interface{})

	children = t.appendNode(children, t.mapPackageDecl(file))
	var nodeListDecls []*Node
	for i := 0; i < len(file.Decls); i++ {
		nodeListDecls = t.appendNode(nodeListDecls, t.mapDecl(file.Decls[i], "["+strconv.Itoa(i)+"]"))
	}
	children = t.appendNodeList(children, nodeListDecls, "Decls([]Decl)")
	slangField["declarations"] = t.filterOutComments(children)
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

	receivers := t.mapFieldListParams(decl.Recv, "Recv")
	children = t.appendNode(children, receivers)
	nativeChildren = t.appendNode(nativeChildren, receivers)

	funcName := t.mapIdent(decl.Name, "Name")
	children = t.appendNode(children, funcName)
	slangField["name"] = funcName

	parameters := t.mapFieldListParams(decl.Type.Params, "Params")
	children = t.appendNode(children, parameters)
	slangField["formalParameters"] = t.getFormalParameter(parameters)

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
	return nil
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
	slangField["identifier"] = parameterIdent
	slangField["type"] = typ
	slangField["modifiers"] = nil    //No paramter modifier in Go
	slangField["defaultValue"] = nil //No default value in Go
	return t.createNode(ident, children, fieldName+"(Parameter)", "Parameter", slangField)
}

func (t *SlangMapper) mapStmtImpl(stmt ast.Stmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapImportSpecImpl(spec *ast.ImportSpec, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapTypeSpecImpl(spec *ast.TypeSpec, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapValueSpecImpl(spec *ast.ValueSpec, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapExprImpl(expr ast.Expr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapAssignStmtImpl(stmt *ast.AssignStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBadStmtImpl(stmt *ast.BadStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBranchStmtImpl(stmt *ast.BranchStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapCaseClauseImpl(clause *ast.CaseClause, fieldName string) *Node {
	return nil
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
	return nil
}

func (t *SlangMapper) mapGoStmtImpl(stmt *ast.GoStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapIfStmtImpl(stmt *ast.IfStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapIncDecStmtImpl(stmt *ast.IncDecStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapLabeledStmtImpl(stmt *ast.LabeledStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapRangeStmtImpl(stmt *ast.RangeStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapSelectStmtImpl(stmt *ast.SelectStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapSendStmtImpl(stmt *ast.SendStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapSwitchStmtImpl(stmt *ast.SwitchStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapTypeSwitchStmtImpl(stmt *ast.TypeSwitchStmt, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapArrayTypeImpl(arrayType *ast.ArrayType, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBadExprImpl(expr *ast.BadExpr, fieldName string) *Node {
	return nil
}

func (t *SlangMapper) mapBinaryExprImpl(expr *ast.BinaryExpr, fieldName string) *Node {
	return nil
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

func (t *SlangMapper) mapFuncLitImpl(lit *ast.FuncLit, fieldName string) *Node {
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
	return nil
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
	return nil
}
