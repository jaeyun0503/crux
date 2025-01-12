grammar Crux;

// parser rules start w/ lowercase
literal
 : Integer
 | True
 | False
 ;

designator
 : Identifier (Open_Bracket expr0 Close_Bracket)?
 ;

type
 : Identifier
 ;

op0
 : Greater_Equal
 | Lesser_Equal
 | Not_Equal
 | Equal
 | Greater_Than
 | Less_Than
 ;

op1
 : Add
 | Sub
 | Or
 ;

op2
 : Mul
 | Div
 | And
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
 : Not expr3
 | Open_Paren expr0 Close_Paren
 | designator
 | callExpr
 | literal
 ;

callExpr
 : Identifier Open_Paren exprList Close_Paren
 ;

exprList
 : (expr0 (Comma expr0)*)?
 ;

param
 : Identifier Colon type
 ;

paramList
 : (param (Comma param)*)?
 ;

varDecl
 : Let Identifier Colon type Semicolon
 ;

arrayDecl
 : Let Identifier Colon Open_Bracket type Semicolon Integer Close_Bracket Semicolon
 ;

functionDefn
 : Fn Identifier Open_Paren paramList Close_Paren (Arrow type)? stmtBlock
 ;

decl
 : varDecl
 | arrayDecl
 | functionDefn
 ;

declList
 : decl*
 ;

assignStmt
 : designator Assign expr0 Semicolon
 ;

callStmt
 : callExpr Semicolon
 ;

ifStmt
 : If expr0 stmtBlock (Else stmtBlock)?
 ;

whileStmt
 : While expr0 stmtBlock
 ;

breakStmt
 : Break Semicolon
 ;

continueStmt
 : Continue Semicolon
 ;

returnStmt
 : Return expr0 Semicolon
 ;

stmt
 : varDecl
 | callStmt
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
 : Open_Brace stmtList Close_Brace
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
