grammar SLang;

slangFile
  :  typeDeclaration* EOF
  ;

typeDeclaration
  :  classDeclaration
  |  methodDeclaration
  |  controlBlock SEMICOLON
  ;

classDeclaration
  :  CLASS identifier? LCURLY typeDeclaration* RCURLY
  ;

methodDeclaration
  :  methodModifier* methodHeader methodBody
  ;

methodModifier
  : PUBLIC  
  | PRIVATE
  ;

methodHeader
  :  simpleType? FUN methodDeclarator
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
  :  simpleType? declarationModifier identifier ('=' expression)?
  ;

declarationModifier
  : VAR
  | VAL
  ;

assignment
  :  expression (assignmentOperator statement)+
  ;

expression
  :  disjunction
  ;

disjunction
  :  conjunction (disjunctionOperator conjunction)*
  ;

conjunction
  :  equalityComparison (conjunctionOperator equalityComparison)*
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
  :  unaryOperator unaryExpression
  |  atomicExpression
  ;

atomicExpression
  :  parenthesizedExpression 
  |  nativeExpression
  |  methodDeclaration
  |  classDeclaration
  |  literal
  |  conditional
  |  loopExpression
  |  methodInvocation
  |  returnExpression
  |  expressionName
  |  tryExpression
  |  jumpExpression
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
  : MATCH LPAREN statement? RPAREN LCURLY matchCase* RCURLY
  ;

matchCase
  :  statement ARROW controlBlock semi
  |  ELSE ARROW controlBlock semi
  ;

loopExpression
  :  forLoop
  |  whileLoop
  |  doWhileLoop
  ;

forLoop
  :  FOR LPAREN declaration RPAREN controlBlock
  ;

whileLoop
  :  WHILE LPAREN statement RPAREN controlBlock
  ;

doWhileLoop
  :  DO controlBlock WHILE LPAREN statement RPAREN
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
  :  RETURN statement?
  ;

jumpExpression
  :  breakExpression
  |  continueExpression
  ;

breakExpression
  : BREAK label?
  ;

continueExpression
  : CONTINUE label?
  ;

label
  : identifier
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

disjunctionOperator
  :  '||'
  ;

conjunctionOperator
  :  '&&'
  ;

// Type Hierarchy

simpleType
  :  identifier
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

CATCH : 'catch';
CLASS : 'class';
DO : 'do';
ELSE : 'else';
FINALLY : 'finally';
FOR : 'for';
FUN: 'fun';
IF : 'if';
MATCH : 'match';
NATIVE : 'native'; 
PRIVATE : 'private';
PUBLIC : 'public';
RETURN : 'return';
THIS : 'this';
TRY : 'try';
VAL : 'val';
VAR : 'var';
WHILE : 'while';
BREAK : 'break';
CONTINUE: 'continue';


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

