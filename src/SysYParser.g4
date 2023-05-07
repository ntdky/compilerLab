parser grammar SysYParser;

options {
     tokenVocab = SysYLexer;
}

program
    :
    compUnit
    ;

compUnit
    :
    (funcDef | decl)+ EOF
    ;

// 声明
decl
    : constDecl
    | varDecl
    ;

// 常量声明
constDecl
    : CONST bType constDef (COMMA constDef)* SEMICOLON
    ;

// 基本类型
bType
    : INT
    ;

// 常数定义
constDef
    : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal
    ;

// 常量初值
constInitVal
    : constExp
    | L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE
    ;

// 变量声明
varDecl
    : bType varDef (COMMA varDef)* SEMICOLON
    ;

// 变量定义
varDef
    : IDENT (L_BRACKT constExp R_BRACKT)* (ASSIGN initVal)?
    ;

// 变量初值
initVal
    : exp
    | L_BRACE (initVal (COMMA initVal)*)? R_BRACE
    ;

// 函数定义
funcDef
    : funcType IDENT L_PAREN funcFParams? R_PAREN block?
    ;

// 函数类型
funcType
    : VOID
    | INT
    ;

funcFParams
    :
    funcFParam (COMMA funcFParam)*
    ;

funcFParam
    :
    bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?
    ;


funcRParams
    : param (COMMA param)*
    ;


param
    : exp
    ;


// 语句块
block
    : L_BRACE blockItem* R_BRACE
    ;

// 语句块项
blockItem
    : decl
    | stmt
    ;


// 语句
stmt
    : lVal ASSIGN exp SEMICOLON
    | exp? SEMICOLON
    | block
    | IF L_PAREN cond R_PAREN stmt (ELSE stmt)?
    | WHILE L_PAREN cond R_PAREN stmt
    | BREAK SEMICOLON
    | CONTINUE SEMICOLON
    | RETURN exp? SEMICOLON
    ;

exp
   : L_PAREN exp R_PAREN
   | lVal
   | number
   | IDENT L_PAREN funcRParams? R_PAREN
   | unaryOp exp
   | exp (MUL | DIV | MOD) exp
   | exp (PLUS | MINUS) exp
   ;

lVal
   : IDENT (L_BRACKT exp R_BRACKT)*
   ;

cond
   : exp
   | cond (LT | GT | LE | GE) cond
   | cond (EQ | NEQ) cond
   | cond AND cond
   | cond OR cond
   ;



number
   : INTEGER_CONST
   ;

unaryOp
   : PLUS
   | MINUS
   | NOT
   ;


constExp
   : exp
   ;
