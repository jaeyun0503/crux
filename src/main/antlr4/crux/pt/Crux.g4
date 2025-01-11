grammar Crux;

// parser rules start w/ lowercase
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
