package easycalc;

import easycalc.grammar.*;
import org.antlr.v4.runtime.Token;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Stack;

public class AnalysisListener extends EasyCalcBaseListener {
    StringBuilder sb = new StringBuilder();

    //to be able to create error strings
    StringBuilder errorString = new StringBuilder();

    //sorted map that is responsible for creating symbol table
    SortedMap<String, String> symbolTable = new TreeMap<>();

    //the sorted map created to ensure that each line has at most one error
    SortedMap<Integer, String> errorMap = new TreeMap<>();

    //stack to hold types of the whole expression or ID
    Stack<String> typeStack = new Stack<>();
    public String getSymbolTableString() {
        return sb.toString();
    }
    public String getErrorMessageString(){
        return errorString.toString();
    }

    public void addRedefErr(Token token) {
        int line = token.getLine();
        int position = token.getCharPositionInLine()+1;

        //to check whether there is already an error for that line, to print at most one error for each line
        if(!errorMap.containsKey(line)) {
            errorString.append("redefinition of ").append(token.getText()).append(" at ").append(line).append(":").append(position).append("\n");
            errorMap.put(line, errorString.toString());
        }
    }

    public void addUndefErr(Token token) {
        int line = token.getLine();
        int position = token.getCharPositionInLine() + 1;

        //to check whether there is already an error for that line, to print at most one error for each line
        if(!errorMap.containsKey(line)) {
            errorString.append(token.getText()).append(" undefined at ").append(line).append(":").append(position).append("\n");
            errorMap.put(line, errorString.toString());
        }
    }

    public void addTypeClashErr(Token token) {
        int line = token.getLine();
        int position = token.getCharPositionInLine() +1;

        //to check whether there is already an error for that line, to print at most one error for each line
        if(!errorMap.containsKey(line)) {
            errorString.append("type clash at ").append(line).append(":").append(position).append("\n");
            errorMap.put(line, errorString.toString());
        }
    }

    public void addArgErr(Token optrToken, Token opndToken, String type) {
        int line = opndToken.getLine();
        int position = opndToken.getCharPositionInLine() +1;

        //to check whether there is already an error for that line, to print at most one error for each line
        if(!errorMap.containsKey(line)) {
            errorString.append(optrToken.getText()).append(" undefined for ").append(type.toUpperCase()).append(" at ").append(line).append(":").append(position).append("\n");
            errorMap.put(line, errorString.toString());
        }

    }


    @Override public void exitAssignStmt(EasyCalcParser.AssignStmtContext ctx) {
        String id = ctx.ID().getText();
        Token token = ctx.ID().getSymbol();

        //checks if the ID is undefined
        if(!symbolTable.containsKey(id)) {
            addUndefErr(token);
        } else{
            //if not undefined then check if expression matches id type
            if (!typeStack.isEmpty()) {
                String exprType = typeStack.pop();
                String idType = symbolTable.get(id);

                if (!idType.equals(exprType)) {
                    addTypeClashErr(token);
                }
            }
        }
    }


    @Override public void exitDeclar(EasyCalcParser.DeclarContext ctx) {
        String id = ctx.ID().getText();
        String type = ctx.type.getText();


        /*to check if the declaration of variable is not repeated, if so it will recognize the error of
        redefinition, if not, then it will put id into sortedTable with its declared type*/
        if(symbolTable.containsKey(id)) {
            addRedefErr(ctx.ID().getSymbol());
        }else {
            symbolTable.put(id, type);
            sb.append(id).append(" -> ").append(type.toUpperCase()).append("\n");
        }

    }
    @Override public void exitReadStmt(EasyCalcParser.ReadStmtContext ctx) {
        String id = ctx.ID().getText();

        //checks if the ID in the statement is undefined
        if (!symbolTable.containsKey(id)) {
            addUndefErr(ctx.ID().getSymbol());
        }
    }

    @Override public void exitWriteStmt(EasyCalcParser.WriteStmtContext ctx) {
        //get expression for write statement
        String expr = ctx.expr().getText();

    }

    @Override public void exitAndExpr(EasyCalcParser.AndExprContext ctx) {
        if (typeStack.size() >= 2) {
            String expr1Type = typeStack.pop();
            String expr2Type = typeStack.pop();

            //to check if expressions are boolean, if not then it will send error
            if (expr1Type.equals("bool") && expr2Type.equals("bool")) {
                typeStack.push("bool");
            } else if(!expr1Type.equals("bool")) {
                addArgErr(ctx.op, ctx.expr().getFirst().start, expr1Type);  // Report type clash for non-boolean operands
            } else if (!expr2Type.equals("bool")) {
                addArgErr(ctx.op, ctx.expr().getLast().start, expr2Type);
            }
        }
    }


    @Override public void exitIfExpr(EasyCalcParser.IfExprContext ctx) {
        if(typeStack.size() >= 3) {
            String expr1Type = typeStack.pop();
            String expr2Type = typeStack.pop();
            String expr3Type = typeStack.pop();

            // if the type of last two expressions dont match it will produce an error
            if (!expr2Type.equals(expr3Type)) {
                addTypeClashErr(ctx.expr(1).getStart());
            }
            // if first expression is not a boolean value, it will produce an error
            if (!expr1Type.equals("bool")) {
                addArgErr(ctx.getStart(), ctx.expr().getFirst().start, expr1Type);
            }
        typeStack.push(expr2Type);
        }

    }

    @Override public void exitMulDivExpr(EasyCalcParser.MulDivExprContext ctx) {
        String op = ctx.op.getText();

        if(typeStack.size() >= 2 ) {
            String expr1Type = typeStack.pop();
            String expr2Type = typeStack.pop();

            //will get the type of the expression and if bool for either of the expressions, it will produce an error
            if (op.equals("*") || op.equals("/")) {
                if (expr1Type.equals("int") && expr2Type.equals("int")) {
                    typeStack.push("int");
                } else if (expr1Type.equals("real") && expr2Type.equals("real")) {
                    typeStack.push("real");
                } else if (expr1Type.equals("bool")) {
                    addArgErr(ctx.op, ctx.expr().getFirst().start, expr1Type);
                } else if (expr2Type.equals("bool")) {
                    addArgErr(ctx.op, ctx.expr().getLast().start, expr2Type);
                } else {
                    addTypeClashErr(ctx.expr(0).getStart());
                }
            }
        }
    }

    @Override public void exitIdExpr(EasyCalcParser.IdExprContext ctx) {
        String id = ctx.ID().getText();

        /*if the id is not in Map, so not defined, then it will produce error, if so, then it will get type of
        the id */
        if(!symbolTable.containsKey(id)) {
            addUndefErr(ctx.ID().getSymbol());
        }else{
            String type = symbolTable.get(id);
            typeStack.push(type);
        }


    }

    @Override public void exitToExpr(EasyCalcParser.ToExprContext ctx) {
        String op = ctx.op.getText();
        String exprType = typeStack.pop();

        // to check if operator is either to int or to real and from there, checking the type if it acceptable with operator
        if(op.equals("to_int")) {
            if (!exprType.equals("real"))
                addArgErr(ctx.op, ctx.expr().getStart(), exprType);
        } else if (op.equals("to_real")) {
            if (!exprType.equals("int"))
                addArgErr(ctx.op, ctx.expr().getStart(), exprType);
        }
    }

    @Override public void exitLitExpr(EasyCalcParser.LitExprContext ctx) {
        String lit = ctx.LIT().getText();

        //checking for literals to determine their type
        if (lit.matches("[0-9]+")) {
            typeStack.push("int");
        } else if(lit.matches("^[0-9]*\\.[0-9]*|[0-9]+\\.[0-9]*|[0-9]*\\.[0-9]+$")) {
            typeStack.push("real");
        } else if(lit.matches("true") || lit.matches("false")) {
            typeStack.push("bool");
        }else {
            System.out.println("error visited in LIT");
            addUndefErr(ctx.LIT().getSymbol());
        }
    }


    @Override public void exitParenExpr(EasyCalcParser.ParenExprContext ctx) {

        //will get the type of whole expression within the parenthesis
        if(!typeStack.isEmpty()) {
            String exprType = typeStack.pop();
            typeStack.push(exprType);
        }

    }

    @Override public void exitAddSubExpr(EasyCalcParser.AddSubExprContext ctx) {
        String op = ctx.op.getText();

        if(typeStack.size() >=2) {
            String expr1Type = typeStack.pop();
            String expr2Type = typeStack.pop();

            // get the type of the expressions but if one of the values are a bool, it will produce an error
            if (op.equals("+") || op.equals("-")) {
                if (expr1Type.equals("int") && expr2Type.equals("int")) {
                    typeStack.push("int");
                } else if (expr1Type.equals("real") && expr2Type.equals("real")) {
                    typeStack.push("real");
                } else if (expr1Type.equals("bool")) {
                    addArgErr(ctx.op, ctx.expr().getFirst().start, expr1Type);
                } else if (expr2Type.equals("bool")) {
                    addArgErr(ctx.op, ctx.expr().getLast().start, expr2Type);
                } else {
                    addTypeClashErr(ctx.expr().getFirst().start);
                }
            }
        }

    }

    @Override public void exitOrExpr(EasyCalcParser.OrExprContext ctx) {
        if (typeStack.size() >= 2) {
            String expr1Type = typeStack.pop();
            String expr2Type = typeStack.pop();

            //to check if expressions are boolean, if not then it will send error
            if (expr1Type.equals("bool") && expr2Type.equals("bool")) {
                typeStack.push("bool");
            } else if(!expr1Type.equals("bool")) {
                addArgErr(ctx.op, ctx.expr().getFirst().start, expr1Type);  // Report type clash for non-boolean operands
            } else if (!expr2Type.equals("bool")) {
                addArgErr(ctx.op, ctx.expr().getLast().start, expr2Type);
            }
        }
    }
}
