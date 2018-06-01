grammar SLang;

slangFile
  :  typeDeclaration* EOF
  ;

typeDeclaration
  :  methodDeclaration 
  |  statementOrExpression
  ;

methodDeclaration
  :  methodModifier* methodHeader methodBody
  ;

methodModifier
  : PUBLIC  
  | PRIVATE
  ;

methodHeader
  :  result? methodDeclarator
  ;

methodDeclarator
  :  identifier LPAREN formalParameterList? RPAREN 
  ;

formalParameterList
  :  formalParameters COMMA lastFormalParameter
  |  lastFormalParameter
  |  receiverParameter
  ;

formalParameters
  :  formalParameter (COMMA formalParameter)*
  ;

formalParameter
  :  simpleType? variableDeclaratorId
  ;

lastFormalParameter
  :  simpleType? ELLIPSIS variableDeclaratorId
  |  formalParameter
  ; 

receiverParameter
  :  simpleType? (identifier DOT)? THIS
  ;

variableDeclaratorId
  :  identifier 
  ;

methodBody
  : block 
  | SEMICOLON 
  ;

block
  :  LCURLY (statementOrExpression semi)* (statementOrExpression semi?)? RCURLY 
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
  |  assignment
  |  methodInvocation
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
  :  statementOrExpression (COMMA statementOrExpression)*
  ; 

expressionName
  :  identifier
  ;

nativeExpression
  :  NATIVE LBRACK argumentList? RBRACK LCURLY nativeBlock* RCURLY 
  ; 

nativeBlock
  :  LBRACK (statementOrExpression semi)* (statementOrExpression semi?)? RBRACK 
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

// Type Hierarchy 

result
  :  simpleType
  |  VOID 
  ;

simpleType
  :  simplePrimitiveType
  |  referenceType
  ;

simplePrimitiveType
  :  numericType
  |  BOOLEAN
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
  :  BYTE 
  |  SHORT 
  |  INT
  |  LONG
  |  CHAR
  ;

floatingPointType
  :  FLOAT
  |  DOUBLE
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
  :  LT typeArgumentList GT
  ;

typeArgumentList
  :  typeArgument (COMMA typeArgument)*
  ;

typeArgument
  :  referenceType
  ;

literal
  :  IntegerLiteral
  |  BooleanLiteral
  |  CharacterLiteral
  |  StringLiteral
  |  NullLiteral
  ;

semi
  :  NL+
  |  SEMICOLON
  |  SEMICOLON NL+
  ;

// LEXER

identifier : Identifier;

// Keywords

BOOLEAN : 'boolean';
BYTE : 'byte';
CHAR : 'char';
DOUBLE : 'double';
FLOAT : 'float';
INT : 'int';
LONG : 'long';
NATIVE : 'native'; 
PRIVATE : 'private';
PUBLIC : 'public';
SHORT : 'short';
THIS : 'this';
VOID : 'void';


// Integer Literals

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

// Boolean Literals

BooleanLiteral
  :  'true'
  |  'false'
  ;

// Character Literals

CharacterLiteral
  :  '\'' SingleCharacter '\''
  ;

fragment
SingleCharacter
  :  ~['\\\r\n]
  ;

// String Literals

StringLiteral
  :  '"' StringCharacters? '"'
  ;

fragment
StringCharacters
  :  StringCharacter+
  ;

fragment
StringCharacter
  :  ~["\\\r\n]
  ;

// The Null Literal

NullLiteral
  :  'null'
  ;

// Separators

COMMA : ',' ;
DOT : '.' ;
ELLIPSIS : '...' ;
LBRACK : '[' ;
LCURLY : '{' ;
LPAREN : '(' ;
RBRACK : ']' ;
RCURLY : '}' ;
RPAREN : ')' ;
SEMICOLON :  ';' ;

// Operators
GT : '>' ;
LT : '<' ;

// Identifiers 

Identifier
  :  SLangLetter SLangLetterOrDigit*
  ;

fragment
SLangLetter
  :  [a-zA-Z$_] 
  ;

fragment
SLangLetterOrDigit
  :  [a-zA-Z0-9$_] 
  ;

// Whitespace and comments 

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

