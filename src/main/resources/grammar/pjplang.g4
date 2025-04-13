grammar pjplang;

program: statement* EOF;

statement
    : type varList ';'
    | expression ';'
    | ID '=' expression ';'
    | 'read' varList ';'
    | 'write' exprList ';'
    | 'if' '(' expression ')' statement ('else' statement)?
    | 'while' '(' expression ')' statement
    | '{' statement* '}'          // ← Důležité pro bloky
    | ';'                         // ← Prázdný statement
    ;

exprList: expression (',' expression)*;

type: 'int' | 'float' | 'bool' | 'string';
varList: ID (',' ID)*;

expression
    : expression '?' expression ':' expression   #TernaryExpr
    | expression op=('*' | '/' | '%') expression #MulDivMod
    | expression op=('+' | '-' | '.') expression #AddSubConcat
    | expression op=('<' | '>') expression       #Relational
    | expression op=('==' | '!=') expression     #Equality
    | expression op='&&' expression              #AndExpr
    | expression op='||' expression              #OrExpr
    | '!' expression                             #NotExpr
    | '-' expression                             #UnaryMinus
    | '(' expression ')'                         #ParenExpr
    | literal                                    #LiteralExpr
    | ID                                         #VarExpr
    ;

literal
    : INT    #IntLit
    | FLOAT  #FloatLit
    | BOOL   #BoolLit
    | STRING #StringLit
    ;

INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]*;
BOOL: 'true' | 'false';
STRING: '"' .*? '"';

ID: [a-zA-Z_][a-zA-Z0-9_]*;

WS: [ \t\r\n]+ -> skip;
COMMENT: '//' ~[\r\n]* -> skip;
