// SonarQube Go Plugin
// Copyright (C) 2018-2022 SonarSource SA
// mailto:info AT sonarsource DOT com
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, write to the Free Software Foundation,
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

package main

//go:generate go run generate_source.go

import (
	"bytes"
	"errors"
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
	"io/ioutil"
	"os"
	"strings"
	"unicode/utf8"
)

type Token struct {
	Value     string     `json:"text"`
	TextRange *TextRange `json:"textRange"`
	TokenType string     `json:"type"`
}

type Node struct {
	Token    *Token  `json:"-"`
	Children []*Node `json:"-"`
	// internal fields
	offset    int // position of first character belonging to the node
	endOffset int // position of first character immediately after the node
	//Slang fields
	SlangType  string                 `json:"@type"`
	TextRange  *TextRange             `json:"metaData"`
	SlangField map[string]interface{} `json:"slangF"`
}

type TextRange struct {
	StartLine   int
	StartColumn int
	EndLine     int
	EndColumn   int
}

const keywordKind = "KEYWORD"
const nativeSlangType = "Native"
const nativeKind = "nativeKind"
const childrenField = "children"
const basicLiteral = "(BasicLit)"
const other = "OTHER"

var isSlangType = map[string]bool{
	other: true, keywordKind: true, "STRING_LITERAL": true}

func toSlangTree(fileSet *token.FileSet, astFile *ast.File, fileContent string) (*Node, []*Node, []*Token) {
	return NewSlangMapper(fileSet, astFile, fileContent).toSlang()
}

func readAstFile(filename string) (fileSet *token.FileSet, astFile *ast.File, fileContent string, err error) {
	var bytes []byte
	if filename == "-" {
		bytes, err = ioutil.ReadAll(os.Stdin)
	} else {
		bytes, err = ioutil.ReadFile(filename)
	}
	if err != nil {
		return
	}
	fileContent = string(bytes)
	fileSet, astFile, err = readAstString(filename, fileContent)
	return
}

func readAstString(filename string, fileContent string) (fileSet *token.FileSet, astFile *ast.File, err error) {
	fileSet = token.NewFileSet()
	astFile, err = parser.ParseFile(fileSet, filename, fileContent, parser.ParseComments)
	if err != nil {
		return
	}
	fileSize := fileSet.File(astFile.Pos()).Size()
	if len(fileContent) != fileSize {
		err = errors.New(fmt.Sprintf("Unexpected file size, expect %d instead of %d for file %s",
			len(fileContent), fileSize, filename))
	}
	return
}

type SlangMapper struct {
	astFile           *ast.File
	fileContent       string
	hasCarriageReturn bool
	file              *token.File
	comments          []*Node
	commentPos        int
	tokens            []*Token
	paranoiac         bool
}

func NewSlangMapper(fileSet *token.FileSet, astFile *ast.File, fileContent string) *SlangMapper {
	t := &SlangMapper{
		astFile:           astFile,
		fileContent:       fileContent,
		hasCarriageReturn: strings.IndexByte(fileContent, '\r') != -1,
		file:              fileSet.File(astFile.Pos()),
		tokens:            nil,
		paranoiac:         true,
	}
	t.comments = t.mapAllComments()
	t.commentPos = 0
	return t
}

func (t *SlangMapper) toSlang() (*Node, []*Node, []*Token) {
	compilationUnit := t.mapFile(t.astFile, "")
	t.addEof(compilationUnit)
	if t.paranoiac && (compilationUnit.offset < 0 || compilationUnit.endOffset > len(t.fileContent)) {
		panic("Unexpected compilationUnit" + t.location(compilationUnit.offset, compilationUnit.endOffset))
	}
	return compilationUnit, t.comments, t.tokens
}

func (t *SlangMapper) addEof(compilationUnit *Node) {
	offset := len(t.fileContent)
	eofNode := t.createToken(offset, offset, "", "EOF")
	compilationUnit.Children = t.appendNode(compilationUnit.Children, eofNode)
	//Update the range of the top level tree to include everything until the end
	compilationUnit.TextRange.EndLine = eofNode.TextRange.EndLine
	compilationUnit.TextRange.EndColumn = eofNode.TextRange.EndColumn
}

func (t *SlangMapper) mapAllComments() []*Node {
	var list []*Node
	for _, commentGroup := range t.astFile.Comments {
		for _, comment := range commentGroup.List {
			node := t.createExpectedToken(comment.Pos(), comment.Text, "", "COMMENT")
			list = append(list, node)
		}
	}
	return list
}

func (t *SlangMapper) mapPackageDecl(file *ast.File) *Node {
	var children []*Node
	// "package" node is the very first node, header comments are appended before
	packageNode := t.createExpectedToken(file.Package, token.PACKAGE.String(), "", keywordKind)
	if packageNode != nil {
		children = t.appendCommentOrMissingToken(children, 0, packageNode.offset)
		children = append(children, packageNode)
	}
	children = t.appendNode(children, t.mapIdent(file.Name, "Name"))

	slangField := make(map[string]interface{})
	slangField[childrenField] = t.filterOutComments(children)

	return t.createNode(file, children, "File.Package", "PackageDeclaration", slangField)
}

func (t *SlangMapper) mapBasicLitTag(astNode *ast.BasicLit, fieldName string) *Node {
	if astNode == nil {
		return nil
	}
	var tokenType = other
	return t.createExpectedToken(astNode.Pos(), astNode.Value, fieldName+basicLiteral, tokenType)
}

func (t *SlangMapper) mapBasicLit(astNode *ast.BasicLit, fieldName string) *Node {
	if astNode == nil {
		return nil
	}
	slangField := make(map[string]interface{})
	var slangType string
	var tokenType = other

	switch astNode.Kind {
	case token.STRING:
		if strings.HasPrefix(astNode.Value, "`") {
			// discard multi-line strings
			return t.createExpectedToken(astNode.Pos(), astNode.Value, fieldName+basicLiteral, tokenType)
		}
		slangType = "StringLiteral"
		tokenType = "STRING_LITERAL"
		slangField["content"] = astNode.Value[1 : len(astNode.Value)-1]
		slangField["value"] = astNode.Value
		return t.createExpectedNode(astNode.Pos(), astNode.Value, fieldName+"(StringLit)", tokenType, slangType, slangField)
	case token.INT:
		slangType = "IntegerLiteral"
		slangField["value"] = astNode.Value
		return t.createExpectedNode(astNode.Pos(), astNode.Value, fieldName+"(IntLit)", tokenType, slangType, slangField)
	default:
		//Binary literal are expected in GO 1.13 (https://github.com/golang/go/issues/19308)
		return t.createExpectedToken(astNode.Pos(), astNode.Value, fieldName+basicLiteral, tokenType)
	}
}

func (t *SlangMapper) appendNode(children []*Node, child *Node) []*Node {
	if child == nil {
		return children
	}
	// Comments are not appended before the first child. They will be appended by an
	// ancestor node before a non first child (except for the "package" node, it's the
	// very first node, it has his specific logic to append header comments)
	if len(children) > 0 {
		lastChild := children[len(children)-1]
		children = t.appendCommentOrMissingToken(children, lastChild.endOffset, child.offset)
		if t.paranoiac && children[len(children)-1].endOffset > child.offset {
			panic("Invalid token sequence" + t.location(children[len(children)-1].endOffset, child.offset))
		}
	}
	return t.appendNodeCheckOrder(children, child)
}

func (t *SlangMapper) createAdditionalInitAndCond(astInit ast.Stmt, astCond ast.Expr) *Node {
	var children []*Node
	children = t.appendNode(children, t.mapStmt(astInit, "Init"))
	children = t.appendNode(children, t.mapExpr(astCond, "Cond"))
	return t.createNativeNode(nil, children, "InitAndCond")
}

func (t *SlangMapper) appendCommentOrMissingToken(children []*Node, offset, endOffset int) []*Node {
	if len(t.comments) == 0 {
		return t.appendMissingToken(children, offset, endOffset)
	}
	// when a child append a comment, it move the 'commentPos' forward, so the parent has to rewind
	for t.commentPos > 0 && t.comments[t.commentPos-1].offset >= offset {
		t.commentPos--
	}

	for t.commentPos < len(t.comments) {
		commentNode := t.comments[t.commentPos]
		if commentNode.offset >= offset {
			if commentNode.endOffset <= endOffset {
				children = t.appendMissingToken(children, offset, commentNode.offset)
				children = t.appendNodeCheckOrder(children, commentNode)
				offset = commentNode.endOffset
			} else {
				break
			}
		}
		t.commentPos++
	}
	return t.appendMissingToken(children, offset, endOffset)
}

func (t *SlangMapper) appendNodeCheckOrder(parentList []*Node, child *Node) []*Node {
	if child == nil {
		return parentList
	}
	if len(parentList) > 0 {
		lastChild := parentList[len(parentList)-1]
		if t.paranoiac && lastChild.endOffset > child.offset {
			panic("Invalid token sequence" + t.location(lastChild.endOffset, child.offset))
		}
	}
	return append(parentList, child)
}

func (t *SlangMapper) appendNodeList(parentList []*Node, children []*Node, nativeNode string) []*Node {
	// TODO provide the next Token offset, so the last separator can be part of the children
	return t.appendNode(parentList, t.createNativeNode(nil, children, nativeNode))
}

func (t *SlangMapper) createNativeNode(astNode ast.Node, children []*Node, nativeNode string) *Node {
	slangField := make(map[string]interface{})
	slangField[childrenField] = t.filterOutComments(children)
	slangField[nativeKind] = nativeNode

	return t.createNode(astNode, children, nativeNode, nativeSlangType, slangField)
}

func (t *SlangMapper) filterOutComments(children []*Node) []*Node {
	//Filter the nodes that are comments
	var slangChildren []*Node
	for _, child := range children {
		if child.Token == nil || isSlangType[child.Token.TokenType] {
			slangChildren = append(slangChildren, child)
		}
	}
	return slangChildren
}

func (t *SlangMapper) createNode(astNode ast.Node, children []*Node, nativeNode, slangType string, slangField map[string]interface{}) *Node {
	if len(children) > 0 {
		return t.createNodeWithChildren(children, slangType, slangField)
	} else if slangField != nil && astNode != nil {
		//We create a leaf node, that is not a Native node
		offset := t.file.Offset(astNode.Pos())
		endOffset := t.file.Offset(astNode.End())
		return t.createLeafNode(offset, endOffset, nativeNode, slangType, other, slangField)
	} else if astNode != nil {
		//We create a node that is a token, required since the original mapping to compute the range
		//In meantime, this node will have slang Native type.
		offset := t.file.Offset(astNode.Pos())
		endOffset := t.file.Offset(astNode.End())
		return t.createToken(offset, endOffset, nativeNode, other)
	} else {
		return nil
	}
}

func (t *SlangMapper) createNativeNodeWithChildren(children []*Node, nativeNode string) *Node {
	slangField := make(map[string]interface{})
	slangField[childrenField] = t.filterOutComments(children)
	slangField[nativeKind] = nativeNode

	return t.createNodeWithChildren(children, nativeSlangType, slangField)
}

func (t *SlangMapper) createNodeWithChildren(children []*Node, slangType string, slangField map[string]interface{}) *Node {
	if len(children) < 1 {
		return nil
	}
	return &Node{
		Children:  children,
		offset:    children[0].offset,
		endOffset: children[len(children)-1].endOffset,
		SlangType: slangType,
		TextRange: &TextRange{
			StartLine:   children[0].TextRange.StartLine,
			StartColumn: children[0].TextRange.StartColumn,
			EndLine:     children[len(children)-1].TextRange.EndLine,
			EndColumn:   children[len(children)-1].TextRange.EndColumn,
		},
		SlangField: slangField,
	}
}

var missingTokens = map[byte]string{
	',': ",", ';': ";", '.': ".", '[': "[", ']': "]", '=': "=", ':': ":",
	't': "type", 'r': "range", 'e': "else", 'c': "chan", '<': "<-"}

var missingTokenNativeNode = map[string]string{
	";": "Semicolon"}

var isMissingTokenKeyword = map[string]bool{
	"else": true, "range": true, "type": true, "chan": true}

func (t *SlangMapper) appendMissingToken(children []*Node, offset, endOffset int) []*Node {
	if offset < 0 || endOffset < offset || endOffset > len(t.fileContent) {
		return nil
	}
	for offset < endOffset && t.fileContent[offset] <= ' ' {
		offset++
	}
	for endOffset > offset && t.fileContent[endOffset-1] <= ' ' {
		endOffset--
	}
	for offset < endOffset {
		missingTokenValue := missingTokens[t.fileContent[offset]]
		tokenLength := len(missingTokenValue)
		var tokenType = other
		if tokenLength == 0 || t.fileContent[offset:offset+tokenLength] != missingTokenValue {
			if t.paranoiac {
				location := t.location(offset, endOffset)
				panic(fmt.Sprintf("Invalid missing token '%s'%s", t.fileContent[offset:endOffset], location))
			}
			tokenLength = endOffset - offset
		} else {
			if isMissingTokenKeyword[missingTokenValue] {
				tokenType = keywordKind
			}
		}
		var nativeNode = missingTokenNativeNode[missingTokenValue]
		missingToken := t.createToken(offset, offset+tokenLength, nativeNode, tokenType)
		children = t.appendNodeCheckOrder(children, missingToken)
		offset += tokenLength
		for offset < endOffset && t.fileContent[offset] <= ' ' {
			offset++
		}
	}
	return children
}

func (t *SlangMapper) createTokenFromPosAstToken(pos token.Pos, tok token.Token, nativeNode string) *Node {
	if pos == token.NoPos {
		return nil
	}
	if !(tok.IsOperator() || tok.IsKeyword()) {
		if t.paranoiac {
			offset := t.file.Offset(pos)
			location := t.location(offset, offset)
			panic(fmt.Sprintf("Unsupported token '%s'%s", tok.String(), location))
		}
		return nil
	}

	return t.createExpectedToken(pos, tok.String(), nativeNode, t.getTokenKind(tok))
}

func (t *SlangMapper) getTokenKind(tok token.Token) string {
	if tok.IsKeyword() {
		return keywordKind
	} else {
		return other
	}
}

func (t *SlangMapper) handleSwitchCase(casePos token.Pos, isDefault bool, children []*Node) []*Node {
	tok := token.CASE
	if isDefault {
		tok = token.DEFAULT
	}
	children = t.appendNode(children, t.createTokenFromPosAstToken(casePos, tok, "Case"))
	return children
}

func (t *SlangMapper) createExpectedToken(pos token.Pos, expectedValue, nativeNode, tokenType string) *Node {
	if pos == token.NoPos {
		return nil
	}
	offset := t.file.Offset(pos)
	var endOffset int
	endOffset, expectedValue = t.computeEndOffsetSupportingMultiLineToken(offset, expectedValue)
	node := t.createToken(offset, endOffset, nativeNode, tokenType)
	if node != nil && node.Token.Value != expectedValue {
		if t.paranoiac {
			location := t.location(offset, endOffset)
			panic(fmt.Sprintf("Invalid token value '%s' instead of '%s'%s",
				node.Token.Value, expectedValue, location))
		}
		return nil
	}
	return node
}

func (t *SlangMapper) createExpectedNode(pos token.Pos, expectedValue, nativeNode, tokenType string, slangType string, slangField map[string]interface{}) *Node {
	if pos == token.NoPos {
		return nil
	}
	offset := t.file.Offset(pos)
	var endOffset int
	endOffset, expectedValue = t.computeEndOffsetSupportingMultiLineToken(offset, expectedValue)
	node := t.createLeafNode(offset, endOffset, nativeNode, slangType, tokenType, slangField)
	if node != nil && node.Token.Value != expectedValue {
		if t.paranoiac {
			location := t.location(offset, endOffset)
			panic(fmt.Sprintf("Invalid token value '%s' instead of '%s'%s",
				node.Token.Value, expectedValue, location))
		}
		return nil
	}
	return node
}

func (t *SlangMapper) computeEndOffsetSupportingMultiLineToken(offset int, value string) (int, string) {
	length := len(value)
	endOffset := offset + length
	if offset < 0 || !t.hasCarriageReturn {
		return endOffset, value
	}
	contentLength := len(t.fileContent)
	// computedEndOffset will be equal to offset + len(value) + <computed number of \r characters>
	computedEndOffset := offset
	for length > 0 && computedEndOffset < contentLength {
		if t.fileContent[computedEndOffset] != '\r' {
			length--
		}
		computedEndOffset++
	}
	if computedEndOffset != endOffset {
		return computedEndOffset, t.fileContent[offset:computedEndOffset]
	}
	return endOffset, value
}

func (t *SlangMapper) createToken(offset, endOffset int, nativeNode, tokenType string) *Node {
	slangField := make(map[string]interface{})
	slangField[nativeKind] = nativeNode

	return t.createLeafNode(offset, endOffset, nativeNode, nativeSlangType, tokenType, slangField)
}

func (t *SlangMapper) createLeafNode(offset, endOffset int, nativeNode, slangType, tokenType string, slangField map[string]interface{}) *Node {
	if offset < 0 || endOffset < offset || endOffset > len(t.fileContent) {
		location := t.location(offset, endOffset)
		panic("Invalid token" + location)
	}
	if endOffset == offset && tokenType != "EOF" {
		if t.paranoiac {
			location := t.location(offset, endOffset)
			panic("Invalid empty token" + location)
		}
		return nil
	}

	startPosition := t.toPosition(offset)
	endPosition := t.toPosition(endOffset)
	if !startPosition.IsValid() || !endPosition.IsValid() {
		if t.paranoiac {
			location := t.location(offset, endOffset)
			panic("Invalid token position" + location)
		}
		return nil
	}
	startLine := startPosition.Line
	startLineOffset := offset - startPosition.Column + 1
	startColumn := utf8.RuneCountInString(t.fileContent[startLineOffset:offset]) + 1

	endLine := endPosition.Line
	endLineOffset := endOffset - endPosition.Column + 1
	endColumn := utf8.RuneCountInString(t.fileContent[endLineOffset:endOffset]) + 1

	if offset > 0 && offset == len(t.fileContent) && isEndOfLine(t.fileContent[offset-1]) {
		startLine++
		startColumn = 1
	}
	if offset > 0 && endOffset == len(t.fileContent) && isEndOfLine(t.fileContent[endOffset-1]) {
		endLine++
		endColumn = 1
	}

	slangToken := &Token{
		TextRange: &TextRange{
			StartLine:   startLine,
			StartColumn: startColumn,
			EndLine:     endLine,
			EndColumn:   endColumn,
		},
		Value: t.fileContent[offset:endOffset],

		TokenType: tokenType,
	}

	if isSlangType[tokenType] {
		t.tokens = append(t.tokens, slangToken)
	}

	return &Node{
		Token:     slangToken,
		offset:    offset,
		endOffset: endOffset,
		SlangType: slangType,
		TextRange: &TextRange{
			StartLine:   startLine,
			StartColumn: startColumn,
			EndLine:     endLine,
			EndColumn:   endColumn,
		},
		SlangField: slangField,
	}
}

func (t *SlangMapper) toPosition(offset int) token.Position {
	position := t.file.Position(t.file.Pos(offset))
	if t.paranoiac && !position.IsValid() {
		panic("Invalid offset" + t.location(offset, offset))
	}
	return position
}

func (t *SlangMapper) location(offset, endOffset int) string {
	var out bytes.Buffer
	out.WriteString(fmt.Sprintf(" at offset %d:%d for file %s", offset, endOffset, t.file.Name()))
	if 0 <= offset && offset <= t.file.Size() {
		p := t.file.Position(t.file.Pos(offset))
		out.WriteString(fmt.Sprintf(":%d:%d", p.Line, p.Column))
	}
	return out.String()
}

func isEndOfLine(ch byte) bool {
	return ch == '\n' || ch == '\r'
}
