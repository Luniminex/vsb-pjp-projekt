grammar pjplang;

program: statement* EOF;

statement
    : ';'  #Empty
    | type varList ';' #Declaration
    | 'read' varList ';' #Read
    | 'write' exprList ';' #Write
    | expression ';' #ExprStmt
    | 'if' '(' expression ')' statement ('else' statement)? #If
    | 'while' '(' expression ')' statement #While
    | '{' statement* '}' #Block
    | 'for' '(' (type IDENT '=' expression | expression)? ';' expression? ';' expression? ')' statement #For
    ;

type: 'int' | 'float' | 'bool' | 'string';
varList: IDENT (',' IDENT)*;
exprList: expression (',' expression)*;

expression
    : IDENT '=' expression #Assign
    | expression op=('+' | '-' | '*' | '/' | '%') expression #Arith
    | expression op=('<' | '>') expression #Relational
    | expression op=('==' | '!=') expression #Compare
    | expression op=('&&' | '||') expression #Logic
    | '!' expression #Not
    | '-' expression #UnaryMinus
    | expression '.' expression #Concat
    | '(' expression ')' #Paren
    | literal #LiteralExpr
    | IDENT #VarExpr
    ;

literal: INT | FLOAT | BOOL | STRING;

BOOL: 'true' | 'false';
INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+;
STRING: '"' .*? '"';
IDENT: [a-zA-Z][a-zA-Z0-9]*;

WS: [ \t\r\n]+ -> skip;
COMMENT: '//' ~[\r\n]* -> skip;
