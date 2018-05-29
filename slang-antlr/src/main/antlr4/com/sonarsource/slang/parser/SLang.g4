grammar SLang;

slangFile
  :  typeDeclaration* EOF
  ;

typeDeclaration
  :  methodDeclaration
  ;

methodDeclaration
  :  methodModifier* methodHeader methodBody
  ;

methodModifier
  : 'public'
  | 'private'
  ;

methodHeader
  :  result methodDeclarator throws_?
  ;

methodDeclarator
  :  identifier LPAREN formalParameterList? RPAREN dims?
  ;

formalParameterList
  :  formalParameters ',' lastFormalParameter
  |  lastFormalParameter
  |  receiverParameter
  ;

formalParameters
  :  formalParameter (',' formalParameter)*
  ;

formalParameter
  :  simpleType variableDeclaratorId
  ;

lastFormalParameter
  :  simpleType '...' variableDeclaratorId
  |  formalParameter
  ;

receiverParameter
  :  simpleType (identifier '.')? 'this'
  ;

variableDeclaratorId
  :  identifier dims?
  ;

dims
  :  '[' ']' ('[' ']')*
  ;

methodBody
  : block
  | ';'
  ;

block
  :  LCURLY (statementOrExpression semi)* (statementOrExpression semi?)? RCURLY
  ;

statementOrExpressionBlock
  :  statementOrExpression+
  ;

statementOrExpression
  :  disjunction (assignmentOperator disjunction)* semi?
  ;

disjunction
  :  conjunction ('||' conjunction)*
  ;

conjunction
  :  equalityComparison ('&&' equalityComparison)*
  ;

equalityComparison
  :  comparison (equalityOperator comparison)*
  ;

comparison
  :  additiveExpression (comparisonOperator additiveExpression)*
  ;

additiveExpression
  :  multiplicativeExpression (additiveOperator multiplicativeExpression)*
  ;

multiplicativeExpression
  :  atomicExpression (multiplicativeOperator atomicExpression)*
  ;

atomicExpression
  :  parenthesizedExpression
  |  nativeExpression
  |  literal
  |  conditional
  |  assignment
  |  methodInvocation
  |  terminator
  |  expressionName
  ;

parenthesizedExpression
  :  LPAREN statementOrExpression RPAREN
  ;

assignment
  :  leftHandSide assignmentOperator statementOrExpression
  ;

leftHandSide
  :  expressionName
  ;

methodInvocation
  :  methodName LPAREN argumentList? RPAREN
  ;

methodName
  :  identifier
  ;

argumentList
  :  statementOrExpression (',' statementOrExpression)*
  ;

expressionName
  :  identifier
  ;

nativeExpression
  :  NATIVE LPAREN argumentList? RPAREN '{' nativeBlock '}'
  ;

nativeBlock
  :  '||' (statementOrExpression semi)* (statementOrExpression semi?)? '||'
  ;

terminator
  :  'return' statementOrExpression
  ;

conditional
  :  'if' LPAREN statementOrExpression RPAREN controlBlock ('else' controlBlock)?
  |  'match' LPAREN statementOrExpression RPAREN LCURLY matchCase* RCURLY
  ;

matchCase
  :  statementOrExpression '->' controlBlock semi
  |  'else' '->' controlBlock semi
  ;

controlBlock
  :  block
  |  statementOrExpression
  ;

/* Operators */
multiplicativeOperator
  :  '*' | '/' | '%'
  ;

additiveOperator
  :  '+' | '-'
  ;

comparisonOperator
  :  '<' | '>' | '>=' | '<='
  ;

equalityOperator
  :  '!=' | '=='
  ;

assignmentOperator
  :  '=' | '+=' | '-=' | '*=' | '%='
  ;

/* Type Hierarchy */
result
  :  simpleType
  |  'void'
  ;

simpleType
  :  simplePrimitiveType
  |  referenceType
  ;

simplePrimitiveType
  :  numericType
  |  'boolean'
  ;

referenceType
  :  classOrInterfaceType
  |  typeVariable
  ;

numericType
  :  integralType
  |  floatingPointType
  ;

integralType
  :  'byte'
  |  'short'
  |  'int'
  |  'long'
  |  'char'
  ;

floatingPointType
  :  'float'
  |  'double'
  ;

classOrInterfaceType
  :  classType
  |  interfaceType
  ;


classType
  : identifier typeArguments?
  ;

interfaceType
  :  classType
  ;

typeVariable
  : identifier
  ;

typeArguments
  :  '<' typeArgumentList '>'
  ;

typeArgumentList
  :  typeArgument (',' typeArgument)*
  ;

typeArgument
  :  referenceType
  ;

// EXCEPTIONS
throws_
  :  'throws' exceptionTypeList
  ;

exceptionTypeList
  :  exceptionType (',' exceptionType)*
  ;

exceptionType
  :  classType
  |  typeVariable
  ;

// LEXER
semi
  :  NL+
  |  SEMICOLON
  |  SEMICOLON NL+
  ;

identifier
  : Identifier
  ;

literal
  :  IntegerLiteral
  |  BooleanLiteral
  |  NullLiteral
  ;

SEMICOLON
  :  ';'
  ;

IntegerLiteral
  :  DecimalIntegerLiteral
  ;

fragment
DecimalIntegerLiteral
  :  DecimalNumeral
  ;

fragment
DecimalNumeral
  :  '0'
  |  NonZeroDigit Digits?
  ;

fragment
Digits
  :  Digit Digit?
  ;

fragment
Digit
  :  '0'
  |  NonZeroDigit
  ;

fragment
NonZeroDigit
  :  [1-9]
  ;

BooleanLiteral
  :  'true'
  |  'false'
  ;

NullLiteral
  :  'null'
  ;

// IDENTIFIERS

Identifier
  :  JavaLetter JavaLetterOrDigit*
  ;

JavaLetter
  :  [a-zA-Z$_]
  ;

JavaLetterOrDigit
  :  [a-zA-Z0-9$_]
  ;

// BREAK

// WHITESPACE

WS
  :  [ \t\r\n\u000C]+ -> skip
  ;

// COMMENTS
COMMENT
  :  '/*' .*? '*/' -> channel(HIDDEN)
  ;

LINE_COMMENT
  :  '//' ~[\r\n]* -> channel(HIDDEN)
  ;

NL
  :  '\u000D'? '\u000A'
  ;

LPAREN: '(' ;
RPAREN: ')' ;

LCURLY: '{' ;
RCURLY: '}' ;

NATIVE: 'native' ;
