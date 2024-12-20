grammar Crux;
program
 : declList EOF
 ;

declList
 : decl*
 ;

decl
 : varDecl
// | arrayDecl
// | functionDefn
 ;

varDecl
 : Let Identifier ':' type ';'
 ;

type
 : Identifier
 ;

literal
 : Integer
 | True
 | False
 ;


SemiColon: ';';
Colon: ':';

Integer
 : '0'
 | [1-9] [0-9]*
 ;

Let: 'let';

True: 'true';
False: 'false';

Identifier
 : [a-zA-Z] [a-zA-Z0-9_]*
 ;

WhiteSpaces
 : [ \t\r\n]+ -> skip
 ;

Comment
 : '//' ~[\r\n]* -> skip
 ;
