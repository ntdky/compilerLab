import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class parserVisitor extends SysYParserBaseVisitor<Void> {

    private int depth = 0;
    private boolean isError = false;
    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private int localScopeCounter = 0;
    private final List<Object> printMsg = new ArrayList<>();

    public List<Object> getPrintMsg() {
        return printMsg;
    }

    public boolean isError() {
        return isError;
    }

    private static final Map<String, String> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put("CONST", "[orange]");
        COLOR_MAP.put("INT", "[orange]");
        COLOR_MAP.put("VOID", "[orange]");
        COLOR_MAP.put("IF", "[orange]");
        COLOR_MAP.put("ELSE", "[orange]");
        COLOR_MAP.put("WHILE", "[orange]");
        COLOR_MAP.put("BREAK", "[orange]");
        COLOR_MAP.put("CONTINUE", "[orange]");
        COLOR_MAP.put("RETURN", "[orange]");

        COLOR_MAP.put("PLUS", "[blue]");
        COLOR_MAP.put("MINUS", "[blue]");
        COLOR_MAP.put("MUL", "[blue]");
        COLOR_MAP.put("DIV", "[blue]");
        COLOR_MAP.put("MOD", "[blue]");
        COLOR_MAP.put("ASSIGN", "[blue]");
        COLOR_MAP.put("EQ", "[blue]");
        COLOR_MAP.put("NEQ", "[blue]");
        COLOR_MAP.put("LT", "[blue]");
        COLOR_MAP.put("GT", "[blue]");
        COLOR_MAP.put("LE", "[blue]");
        COLOR_MAP.put("GE", "[blue]");
        COLOR_MAP.put("NOT", "[blue]");
        COLOR_MAP.put("AND", "[blue]");
        COLOR_MAP.put("OR", "[blue]");

        COLOR_MAP.put("IDENT", "[red]");

        COLOR_MAP.put("INTEGER_CONST", "[green]");
    }

    private String getColor(String name) {
        return COLOR_MAP.getOrDefault(name, null);
    }


    private String parseNumber(String numberStr) {
        if (numberStr.startsWith("0x") || numberStr.startsWith("0X")) {
            numberStr = String.valueOf(Integer.parseInt(numberStr.substring(2), 16));
        } else if (numberStr.startsWith("0")) {
            numberStr = String.valueOf(Integer.parseInt(numberStr, 8));
        }
        return numberStr;
    }

    private void printError(int typeNo, int lineNo, String message) {
        System.err.println("Error type " + typeNo + " at Line " + lineNo + ": " + message + ".");
        isError = true;
    }

    private String generatePrefix(int depth) {
        return String.join("", Collections.nCopies(depth, "  "));
    }

    @Override
    public Void visitChildren(RuleNode node) {
        RuleContext ctx = node.getRuleContext();
        int ruleIndex = ctx.getRuleIndex();
        String ruleName = SysYParser.ruleNames[ruleIndex];
        String realName = ruleName.substring(0, 1).toUpperCase() + ruleName.substring(1);
        printMsg.add(generatePrefix(depth) + realName);

        depth++;
        Void ret = super.visitChildren(node);
        depth--;

        return ret;
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        Token token = node.getSymbol();
        int ruleNum = token.getType() - 1;

        if (ruleNum < 0) {
            return super.visitTerminal(node);
        }

        String ruleName = SysYLexer.ruleNames[ruleNum];
        String tokenText = token.getText();
        String color = getColor(ruleName);
        Symbol symbol = currentScope.getSymbol(tokenText);

        if (ruleName.equals("INTEGER_CONST")) {
            tokenText = parseNumber(tokenText);
        }

        if (!(color == null)) {
            String temp = generatePrefix(depth);
            if (symbol == null) {
                temp += tokenText;
            } else {
                temp += symbol.getName();
            }
            printMsg.add(temp + " " + ruleName + color);
        }

        return super.visitTerminal(node);
    }

    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
        Void ret = super.visitProgram(ctx);
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        if (currentScope.isHaveSymbol(funcName)) {
            printError(4, getLineNo(ctx), "Redefined function: " + funcName);
            return null;
        }

        String retTypeName = ctx.funcType().getText();
        Type retType = (Type) globalScope.getSymbol(retTypeName);
        ArrayList<Type> paramsType = new ArrayList<>();
        FunctionType functionType = new FunctionType(retType, paramsType);
        FunctionSymbol functionSymbol = new FunctionSymbol(funcName, currentScope, functionType);
        currentScope.putSymbol(functionSymbol);
        currentScope = functionSymbol;
        Void ret = super.visitFuncDef(ctx);
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        LocalScope localScope = new LocalScope(currentScope);
        String localScopeName = localScope.getName() + localScopeCounter;
        localScope.setName(localScopeName);
        localScopeCounter++;
        currentScope = localScope;
        Void ret = super.visitBlock(ctx);
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    int getLineNo(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        String typeName = ctx.bType().getText();
        Type varType = (Type) globalScope.getSymbol(typeName);

        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            String varName = varDefContext.IDENT().getText();
            if (currentScope.isHaveSymbol(varName)) {
                printError(3, getLineNo(varDefContext), "Redefined variable: " + varName);
                continue;
            }

            Type currentVarType = varType;
            for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()) {
                int elementCount = Integer.parseInt(parseNumber(constExpContext.getText()));
                currentVarType = new ArrayType(elementCount, currentVarType);
            }

            if (varDefContext.ASSIGN() != null) {
                checkInitValType(varDefContext, currentVarType);
            }

            VariableSymbol varSymbol = new VariableSymbol(varName, currentVarType);
            currentScope.putSymbol(varSymbol);
        }

        return super.visitVarDecl(ctx);
    }

    private void checkInitValType(SysYParser.VarDefContext varDefContext, Type currentVarType) {
        SysYParser.ExpContext expContext = varDefContext.initVal().exp();
        if (expContext != null) {
            Type initValType = determineParseExpType(expContext);
            if (!initValType.toString().equals(Constant.NoTYPE) && !currentVarType.toString().equals(initValType.toString())) {
                printError(5, getLineNo(varDefContext), "Type mismatched for assignment");
            }
        }
    }


    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {
        String typeName = ctx.bType().getText();
        Type constType = (Type) globalScope.getSymbol(typeName);

        for (SysYParser.ConstDefContext varDefContext : ctx.constDef()) {
            String constName = varDefContext.IDENT().getText();

            if (currentScope.isHaveSymbol(constName)) {
                printError(3, getLineNo(varDefContext), "Redefined variable: " + constName);
                continue;
            }

            Type currentConstType = constType;
            for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()) {
                int elementCount = Integer.parseInt(parseNumber(constExpContext.getText()));
                currentConstType = new ArrayType(elementCount, currentConstType);
            }
            checkInitValType(varDefContext, currentConstType);

            VariableSymbol constSymbol = new VariableSymbol(constName, currentConstType);
            currentScope.putSymbol(constSymbol);
        }

        return super.visitConstDecl(ctx);
    }

    private void checkInitValType(SysYParser.ConstDefContext varDefContext, Type currentConstType) {
        SysYParser.ConstExpContext expContext = varDefContext.constInitVal().constExp();

        if (expContext != null) {
            Type initValType = determineParseExpType(expContext.exp());
            if (!initValType.toString().equals(Constant.NoTYPE) && !currentConstType.toString().equals(initValType.toString())) {
                printError(5, getLineNo(varDefContext), "Type mismatched for assignment");
            }
        }
    }


    @Override
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        String variableTypeName = ctx.bType().getText();
        Type variableType = (Type) globalScope.getSymbol(variableTypeName);
        for (TerminalNode ignored : ctx.L_BRACKT()) {
            variableType = new ArrayType(0, variableType);
        }
        String variableName = ctx.IDENT().getText();
        VariableSymbol variableSymbol = new VariableSymbol(variableName, variableType);

        if (currentScope.isHaveSymbol(variableName)) {
            printError(3, getLineNo(ctx), "Redefined variable: " + variableName);
        } else {
            currentScope.putSymbol(variableSymbol);
            ((FunctionSymbol) currentScope).getType().getParamsType().add(variableType);
        }
        return super.visitFuncFParam(ctx);
    }

    private Type determineLValType(SysYParser.LValContext ctx) {
        String variableName = ctx.IDENT().getText();
        Symbol symbol = currentScope.getSymbol(variableName);
        if (symbol == null) {
            return new BasicTypeSymbol(Constant.NoTYPE);
        }
        Type varType = symbol.getType();
        for (SysYParser.ExpContext ignored : ctx.exp()) {
            if (varType instanceof ArrayType) {
                varType = ((ArrayType) varType).elementType;
            } else {
                return new BasicTypeSymbol(Constant.NoTYPE);
            }
        }
        return varType;
    }


    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Symbol symbol = currentScope.getSymbol(varName);
        if (symbol == null) {
            printError(1, getLineNo(ctx), "Undefined variable: " + varName);
            return null;
        }
        Type varType = symbol.getType();
        int arrayDimension = ctx.exp().size();
        for (int i = 0; i < arrayDimension; i++) {
            if (varType instanceof ArrayType) {
                varType = ((ArrayType) varType).elementType;
                SysYParser.ExpContext expContext = ctx.exp(i);
                varName += "[" + expContext.getText() + "]";
            } else {
                printError(9, getLineNo(ctx), "Not an array: " + varName);
                break;
            }
        }

        return super.visitLVal(ctx);
    }

    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.ASSIGN() != null) {
            Type lValType = determineLValType(ctx.lVal());
            Type rValType = determineParseExpType(ctx.exp());
            validateAssignment(lValType, rValType, ctx);
        } else if (ctx.RETURN() != null) {
            Type retType = new BasicTypeSymbol(Constant.VOID);
            if (ctx.exp() != null) {
                retType = determineParseExpType(ctx.exp());
            }
            validateReturn(retType, ctx);
        }
        return super.visitStmt(ctx);
    }

    private void validateAssignment(Type lValType, Type rValType, SysYParser.StmtContext ctx) {
        if (lValType instanceof FunctionType) {
            printError(11, getLineNo(ctx), "The left-hand side of an assignment must be a variable");
        } else if (!lValType.toString().equals(Constant.NoTYPE) && !rValType.toString().equals(Constant.NoTYPE) && !lValType.toString().equals(rValType.toString())) {
            printError(5, getLineNo(ctx), "Type mismatched for assignment");
        }
    }

    private void validateReturn(Type retType, SysYParser.StmtContext ctx) {
        Scope temp = currentScope;
        while (!(temp instanceof FunctionSymbol)) {
            temp = temp.getEnclosingScope();
        }

        Type expectedType = ((FunctionSymbol) temp).getType().getRetType();
        if (!retType.toString().equals(Constant.NoTYPE) && !expectedType.toString().equals(Constant.NoTYPE) && !retType.toString().equals(expectedType.toString())) {
            printError(7, getLineNo(ctx), "Type mismatched for return");
        }
    }


    private Type determineParseExpType(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) {
            return handleFunctionCall1(ctx);
        } else if (ctx.L_PAREN() != null) {
            return determineParseExpType(ctx.exp(0));
        } else if (ctx.unaryOp() != null) {
            return determineParseExpType(ctx.exp(0));
        } else if (ctx.lVal() != null) {
            return determineLValType(ctx.lVal());
        } else if (ctx.number() != null) {
            return new BasicTypeSymbol(Constant.INT);
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            return handleBinaryOperation1(ctx);
        }
        return new BasicTypeSymbol(Constant.NoTYPE);
    }

    private Type handleFunctionCall1(SysYParser.ExpContext ctx) {
        String funcName = ctx.IDENT().getText();
        Symbol symbol = currentScope.getSymbol(funcName);
        if (symbol != null && symbol.getType() instanceof FunctionType) {
            FunctionType functionType = (FunctionType) currentScope.getSymbol(funcName).getType();
            ArrayList<Type> paramsType = functionType.getParamsType();
            ArrayList<Type> argsType = new ArrayList<>();
            if (ctx.funcRParams() != null) {
                for (SysYParser.ParamContext paramContext : ctx.funcRParams().param()) {
                    argsType.add(determineParseExpType(paramContext.exp()));
                }
            }
            if (paramsType.equals(argsType)) {
                return functionType.getRetType();
            }
        }
        return new BasicTypeSymbol(Constant.NoTYPE);
    }

    private Type handleBinaryOperation1(SysYParser.ExpContext ctx) {
        Type firstOpType = determineParseExpType(ctx.exp(0));
        Type secondOpType = determineParseExpType(ctx.exp(1));
        if (firstOpType.toString().equals(Constant.INT) && secondOpType.toString().equals(Constant.INT)) {
            return firstOpType;
        }
        return new BasicTypeSymbol(Constant.NoTYPE);
    }


    private boolean checkArgsTypes(ArrayList<Type> paramsType, ArrayList<Type> argsType) {
        if (paramsType.size() != argsType.size()) {
            return false;
        }
        for (int i = 0; i < paramsType.size(); ++i) {
            Type param = paramsType.get(i);
            Type arg = argsType.get(i);
            if (param.toString().equals(Constant.NoTYPE) || arg.toString().equals(Constant.NoTYPE)) {
                return true;
            }
            if (!param.toString().equals(arg.toString())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) {
            handleFunctionCall(ctx);
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            handleBinaryOperation(ctx);
        } else if (ctx.unaryOp() != null) {
            handleUnaryOperation(ctx);
        }
        return super.visitExp(ctx);
    }

    private void handleFunctionCall(SysYParser.ExpContext ctx) {
        String functionName = ctx.IDENT().getText();
        Symbol symbol = currentScope.getSymbol(functionName);
        if (symbol == null) {
            printError(2, getLineNo(ctx), "Undefined function: " + functionName);
        } else if (!(symbol.getType() instanceof FunctionType)) {
            printError(10, getLineNo(ctx), "Not a function: " + functionName);
        } else {
            FunctionType functionType = (FunctionType) symbol.getType();
            ArrayList<Type> paramsType = functionType.getParamsType();
            ArrayList<Type> argsType = new ArrayList<>();
            if (ctx.funcRParams() != null) {
                for (SysYParser.ParamContext paramContext : ctx.funcRParams().param()) {
                    argsType.add(determineParseExpType(paramContext.exp()));
                }
            }
            if (!checkArgsTypes(paramsType, argsType)) {
                printError(8, getLineNo(ctx), "Function is not applicable for arguments");
            }
        }
    }

    private void handleBinaryOperation(SysYParser.ExpContext ctx) {
        Type firstOpType = determineParseExpType(ctx.exp(0));
        Type secondOpType = determineParseExpType(ctx.exp(1));
        boolean isTypeValid = ((firstOpType.toString().equals(Constant.NoTYPE) || secondOpType.toString().equals(Constant.NoTYPE))
                || (firstOpType.toString().equals(Constant.INT) && secondOpType.toString().equals(Constant.INT)));
        if (!isTypeValid) {
            printError(6, getLineNo(ctx), "Type mismatched for operands");
        }
    }

    private void handleUnaryOperation(SysYParser.ExpContext ctx) {
        Type expType = determineParseExpType(ctx.exp(0));
        if (!expType.toString().equals(Constant.INT)) {
            printError(6, getLineNo(ctx), "Type mismatched for operands");
        }
    }


    private Type determineCondType(SysYParser.CondContext ctx) {
        if (ctx.exp() != null) {
            return determineParseExpType(ctx.exp());
        }

        Type firstCond = determineCondType(ctx.cond(0));
        Type secondCond = determineCondType(ctx.cond(1));
        if (firstCond.toString().equals(Constant.INT) && secondCond.toString().equals(Constant.INT)) {
            return firstCond;
        }
        return new BasicTypeSymbol(Constant.NoTYPE);
    }

    @Override
    public Void visitCond(SysYParser.CondContext ctx) {
        boolean isTypeMismatched = ctx.exp() == null && !determineCondType(ctx).toString().equals(Constant.INT);
        if (isTypeMismatched) {
            printError(6, getLineNo(ctx), "Type mismatched for operands");
        }
        return super.visitCond(ctx);
    }
}
