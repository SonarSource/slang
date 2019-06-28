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
	slangField["declarations"] = children
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
	return nil
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

func (t *SlangMapper) mapBlockStmtImpl(stmt *ast.BlockStmt, fieldName string) *Node {
	return nil
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
	return nil
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
