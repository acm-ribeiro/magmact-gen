grammar magmact;

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
	| LCURL? param RCURL?
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
	: operationHeader LPAR operationParameter RPAR operationSuffix?
	| pathParameter
	| queryParameter
	;

operationSuffix
	: function
	| LCURL stringParam RCURL
	;

operationHeader
	: 'request_body'
	| 'response_body'
	| 'response_code'
	;

pathParameter
	: 'path_param' LPAR THIS LSTR8 INT RSTR8 RPAR
	;

queryParameter
	: 'query_param' LPAR THIS RPAR LCURL STRING RCURL
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
	| 'True'
	| 'False'
	| 'true'
	| 'false'
	;

stringParam
	: STRING ('.' STRING)*
	;

param
	: stringParam
	| INT
	;

segment
	: RBAR block( '.' block)*
	;

block
	: LCURL blockParameter RCURL
	| STRING
	| INT
	| operation
	;

blockParameter
	: STRING ('.' STRING)?
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
	: [0-9_@\-]*[A-Za-z_@]+[A-Za-z0-9_@\-]*
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

LSTR8
	: '['
	;

RSTR8
	: ']'
	;


NEWLINE
	: ('\r'? '\n' | '\r')+
	;

WS
	: ' ' -> skip
	;