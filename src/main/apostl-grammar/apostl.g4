grammar apostl ;


formula
	: quantifiedFormula
	| booleanExpression
	;

quantifiedFormula
	: quantifier vars ':-' booleanExpression
	| quantifier vars ':'  quantifiedFormula
	;

quantifier
	: 'for'
	| 'exists'
	;

vars
	: varID 'in' call
	| varID ',' vars
	;

call
	: operation
	| operationPrevious
	;

booleanExpression
	: booleanExpression booleanOperator booleanExpression
	| clause
	;

clause
	: booleanValue
	| comparison
	;

comparison
	: leftTerm comparator rightTerm
	;

leftTerm
	: call
	| param
	;

rightTerm
	: leftTerm
	| noValue
	;

noValue
	: 'null'
	| 'nil'
	| 'none'
	;

operationPrevious
	: previousHeader LPAR operation RPAR
	;

operation
	: operationHeader LPAR operationParameter RPAR function?
	;

operationHeader
	: 'request_body'
	| 'response_body'
	| 'response_code'
	;

operationParameter
	: httpRequest
	| THIS
	;

httpRequest
	: method url
	;

url
	: segment+
	;

method
	: 'GET'
	| 'POST'
	| 'PUT'
	| 'DELETE'
	;

comparator
	: '=='
	| '!='
	| '<='
	| '>='
	| '<'
	| '>'
	;

booleanOperator
	: '&&'
	| '||'
	| '=>'
	;

booleanValue
	: 'T'
	| 'F'
	;

param
	: STRING ('.' STRING)*
	| INT
	;

segment
	: RBAR block( '.' block)*
	;

block
	: LCURL blockParameter RCURL
	| STRING
	;

blockParameter
	: STRING ('.' STRING)?
	| call
	;

function
	: '.' functionID
	;

functionID
	: STRING
	;

varID
	: STRING
	;

previousHeader
	: 'previous'
	;

THIS
	: 'this'
	;

STRING
	: [A-Za-z0-9]+
	;

INT
	: '-'? [0-9]+
	;

RBAR
	: '/'
	;

LCURL
	: '{'
	;

RCURL
	: '}'
	;

LPAR
	: '('
	;

RPAR
	: ')'
	;

NEWLINE
	: ('\r'? '\n' | '\r')+
	;

WS
	: ' ' -> skip
	;