grammar Crux;

// parser rules start w/ lowercase
literal
 : Integer
 | True
 | False
 ;

designator
 : Identifier ('[' expr0 ']')?
 ;

type
 : Identifier
 ;

op0
 : '>='
 | '<='
 | '!='
 | '=='
 | '>'
 | '<'
 ;

op1
 : '+'
 | '-'
 | '||'
 ;

op2
 : '*'
 | '/'
 | '&&'
 ;

expr0
 : expr1 (op0 expr1)?
 ;

expr1
 : expr2
 | expr1 op1 expr2
 ;

expr2
 : expr3
 | expr2 op2 expr3
 ;

expr3
 : '!' expr3
 | '(' expr0 ')'
 | designator
 | callExpr
 | literal
 ;

callExpr
 : Identifier '(' exprList ')'
 ;

exprList
 : (expr0 (',' expr0)*)?
 ;

param
 : Identifier ':' type
 ;

paramList
 : (param (',' param)*)?
 ;

varDecl
 : Let Identifier ':' type ';'
 ;

arrayDecl
 : Let Identifier ':' '[' type ';' Integer ']' ';'
 ;


program
 : declList EOF
 ;

 functionDefn
 : Fn Identifier '(' paramList ')' ('->' type)? stmtBlock
 ;

decl
 : varDecl
// | arrayDecl
 | functionDefn
 ;

declList
 : decl*
 ;

stmt
 : callStmt
 | assignStmt
 | ifStmt
 | whileStmt
 | breakStmt
 | continueStmt
 | returnStmt
 ;

stmtList
 : stmt*
 ;

stmtBlock
 : '{' stmtList '}'
 ;

program
 : declList EOF
 ;


// lexer rules start w/ uppercase
// reserved keywords
And: '&&';
Or: '||';
Not: '!';
Let: 'let';
Fn: 'fn';
If: 'if';
Else: 'else';
While: 'while';
Break: 'break';
Continue: 'continue';
True: 'true';
False: 'false';
Return: 'return';

// character sequences w/ special meaning
Open_Paren: '(';
Close_Paren: ')';
Open_Brace: '{';
Close_Brace: '}';
Open_Bracket: '[';
Close_Bracket: ']';
Add: '+';
Sub: '-';
Mul: '*';
Div: '/';
Greater_Equal: '>=';
Lesser_Equal: '<=';
Not_Equal: '!=';
Equal: '==';
Greater_Than: '>';
Less_Than: '<';
Assign: '=';
Comma: ',';
Colon: ':';
Semicolon: ';';
Arrow: '->';

// reserved value literals
Integer
 : '0'
 | [1-9] [0-9]*
 ;
Identifier
 : [a-zA-Z] [a-zA-Z0-9_]*
 ;

// ignore whitespaces and comments
WhiteSpaces
 : [ \t\r\n]+ -> skip
 ;
Comment
 : '//' ~[\r\n]* -> skip
 ;
