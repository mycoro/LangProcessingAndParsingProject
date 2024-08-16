grammar SimpleCalc;

program : stmt '$$' ;

stmt    : ID ':=' expr          # AssignStmt
        | 'read' ID             # ReadStmt
        | 'write' expr          # WriteStmt
        ;

expr    : ID                    # IdentExpr
        | NUMBER                # NumberExpr
        | '(' expr ')'          # ParenExpr
        | expr ('*' | '/') expr # MulDivExpr
        | expr ('+' | '-') expr # AddSubExpr
        ;

// tokens

READ    : 'read' ;
WRITE   : 'write' ;
LPAREN  : '(' ;
RPAREN  : ')' ;
ASSIGN  : ':=' ;
ADD     : '+' ;
SUB     : '-' ;
MUL     : '*' ;
DIV     : '/' ;
STOP    : '$$' ;

ID      : [a-z]+ ;
NUMBER  : [0-9]+ ;

WS		: [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines
