grammar SLang;

slangFile
  :  typeDeclaration* EOF
  ;

typeDeclaration
  :  methodDeclaration 
  |  statement SEMICOLON
  ;

methodDeclaration
  :  methodModifier* methodHeader methodBody
  ;

methodModifier
  : PUBLIC  
  | PRIVATE
  ;

methodHeader
  :  result? FUN methodDeclarator
  ;

methodDeclarator
  :  identifier? LPAREN formalParameterList? RPAREN
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
  :  LCURLY (statement semi)* (statement semi?)? RCURLY
  ;

statement
  :  declaration
  |  assignment
  |  expression
  ;

declaration
  :  simpleType identifier
  ;

assignment
  :  expression (assignmentOperator statement)+
  ;

expression
  :  disjunction
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
  :  unaryExpression (multiplicativeOperator unaryExpression)*
  ;

unaryExpression
  :  unaryOperator atomicExpression
  |  atomicExpression
  ;

atomicExpression
  :  parenthesizedExpression 
  |  nativeExpression 
  |  literal
  |  conditional
  |  methodInvocation
  |  returnExpression
  |  expressionName
  |  tryExpression
  ;

parenthesizedExpression
  :  LPAREN statement RPAREN
  ;

methodInvocation
  :  methodName LPAREN argumentList? RPAREN 
  ;

methodName 
  :  identifier
  ;

argumentList
  :  statement (COMMA statement)*
  ; 

expressionName
  :  identifier
  ;

conditional
  :  ifExpression
  |  matchExpression
  ;

ifExpression
  : IF LPAREN statement RPAREN controlBlock (ELSE controlBlock)?
  ;

matchExpression
  : MATCH LPAREN statement RPAREN LCURLY matchCase* RCURLY
  ;

matchCase
  :  statement ARROW controlBlock semi
  |  ELSE ARROW controlBlock semi
  ;

controlBlock
  :  block
  |  statement
  ;

tryExpression
  : TRY block catchBlock* finallyBlock?
  ;

catchBlock
  : CATCH LPAREN formalParameter? RPAREN block
  ;

finallyBlock
  : FINALLY block
  ;

nativeExpression
  :  NATIVE LBRACK argumentList? RBRACK LCURLY nativeBlock* RCURLY 
  ; 

nativeBlock
  :  LBRACK (statement semi)* (statement semi?)? RBRACK
  ;

returnExpression
  :  RETURN statement
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

unaryOperator
  :  '!'
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
CATCH : 'catch';
CHAR : 'char';
DOUBLE : 'double';
ELSE : 'else';
FINALLY : 'finally';
FLOAT : 'float';
FUN: 'fun';
IF : 'if';
INT : 'int';
LONG : 'long';
MATCH : 'match';
NATIVE : 'native'; 
PRIVATE : 'private';
PUBLIC : 'public';
RETURN : 'return';
SHORT : 'short';
THIS : 'this';
TRY : 'try';
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
  |  NonZeroDigit Digit*
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

ARROW : '->' ;
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
  :  '/*' .*? '*/' -> channel(1)
  ;

LINE_COMMENT
  :  '//' ~[\r\n]* -> channel(1)
  ;

NL
  :  '\u000D'? '\u000A'
  ;

