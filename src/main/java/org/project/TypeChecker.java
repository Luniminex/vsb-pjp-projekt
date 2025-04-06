package org.project;


import grammar.pjplangBaseVisitor;
import grammar.pjplangParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeChecker extends pjplangBaseVisitor<Type> {

    private final Map<String, Type> symbolTable = new HashMap<>();
    private final List<String> errors = new ArrayList<>();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void printErrors() {
        for (String e : errors) {
            System.err.println("Type error: " + e);
        }
    }

    // ----------- STATEMENTS -----------

    @Override
    public Type visitProgram(pjplangParser.ProgramContext ctx) {
        for (var stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Type visitDeclaration(pjplangParser.DeclarationContext ctx) {
        Type declaredType = Type.fromString(ctx.type().getText());

        for (TerminalNode ident : ctx.varList().IDENT()) {
            String varName = ident.getText();
            if (symbolTable.containsKey(varName)) {
                errors.add("Variable '" + varName + "' already declared.");
            } else {
                symbolTable.put(varName, declaredType);
            }
        }
        return null;
    }

    @Override
    public Type visitExprStmt(pjplangParser.ExprStmtContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Type visitRead(pjplangParser.ReadContext ctx) {
        for (TerminalNode ident : ctx.varList().IDENT()) {
            if (!symbolTable.containsKey(ident.getText())) {
                errors.add("Variable '" + ident.getText() + "' used before declaration.");
            }
        }
        return null;
    }

    @Override
    public Type visitWrite(pjplangParser.WriteContext ctx) {
        for (var expr : ctx.exprList().expression()) {
            visit(expr);
        }
        return null;
    }

    @Override
    public Type visitIf(pjplangParser.IfContext ctx) {
        Type cond = visit(ctx.expression());
        if (cond != Type.BOOL) {
            errors.add("Condition in if must be bool.");
        }

        visit(ctx.statement(0));
        if (ctx.statement().size() > 1) {
            visit(ctx.statement(1));
        }

        return null;
    }

    @Override
    public Type visitWhile(pjplangParser.WhileContext ctx) {
        Type cond = visit(ctx.expression());
        if (cond != Type.BOOL) {
            errors.add("Condition in while must be bool.");
        }
        visit(ctx.statement());
        return null;
    }

    @Override
    public Type visitFor(pjplangParser.ForContext ctx) {
        if (ctx.expression(0) != null && ctx.type() != null && ctx.IDENT() != null) {
            String varName = ctx.IDENT().getText();
            Type declaredType = Type.fromString(ctx.type().getText());
            Type valueType = visit(ctx.expression(0));

            if (symbolTable.containsKey(varName)) {
                errors.add("Variable '" + varName + "' already declared.");
            } else if (!TypeCheckerHelper.isAssignable(declaredType, valueType)) {
                errors.add("Cannot assign " + valueType + " to declared variable of type " + declaredType);
            } else {
                symbolTable.put(varName, declaredType);
            }
        } else if (ctx.expression(0) != null) {
            visit(ctx.expression(0));
        }

        if (ctx.expression(1) != null) {
            Type condType = visit(ctx.expression(1));
            if (condType != Type.BOOL) {
                errors.add("Condition in for must be of type bool.");
            }
        }

        if (ctx.expression(2) != null) {
            visit(ctx.expression(2));
        }

        visit(ctx.statement());

        return null;
    }

    @Override
    public Type visitBlock(pjplangParser.BlockContext ctx) {
        for (var stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    // ----------- EXPRESSIONS -----------

    @Override
    public Type visitAssign(pjplangParser.AssignContext ctx) {
        String varName = ctx.IDENT().getText();
        if (!symbolTable.containsKey(varName)) {
            errors.add("Variable '" + varName + "' used before declaration.");
            return Type.ERROR;
        }

        Type varType = symbolTable.get(varName);
        Type valueType = visit(ctx.expression());

        if (!TypeCheckerHelper.isAssignable(varType, valueType)) {
            errors.add("Cannot assign " + valueType + " to " + varType + " variable '" + varName + "'.");
            return Type.ERROR;
        }

        return varType;
    }

    @Override
    public Type visitVarExpr(pjplangParser.VarExprContext ctx) {
        String varName = ctx.IDENT().getText();
        if (!symbolTable.containsKey(varName)) {
            errors.add("Variable '" + varName + "' used before declaration.");
            return Type.ERROR;
        }
        return symbolTable.get(varName);
    }

    @Override
    public Type visitLiteralExpr(pjplangParser.LiteralExprContext ctx) {
        if (ctx.literal().INT() != null) return Type.INT;
        if (ctx.literal().FLOAT() != null) return Type.FLOAT;
        if (ctx.literal().BOOL() != null) return Type.BOOL;
        if (ctx.literal().STRING() != null) return Type.STRING;
        return Type.ERROR;
    }

    @Override
    public Type visitParen(pjplangParser.ParenContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Type visitUnaryMinus(pjplangParser.UnaryMinusContext ctx) {
        Type type = visit(ctx.expression());
        if (type == Type.INT || type == Type.FLOAT) return type;
        errors.add("Unary minus applied to non-numeric type: " + type);
        return Type.ERROR;
    }

    @Override
    public Type visitNot(pjplangParser.NotContext ctx) {
        Type type = visit(ctx.expression());
        if (type == Type.BOOL) return Type.BOOL;
        errors.add("Logical not (!) requires bool type, found: " + type);
        return Type.ERROR;
    }

    @Override
    public Type visitArith(pjplangParser.ArithContext ctx) {
        Type left = visit(ctx.expression(0));
        Type right = visit(ctx.expression(1));

        if (!left.isNumeric() || !right.isNumeric()) {
            errors.add("Arithmetic operator '" + ctx.op.getText() + "' used with non-numeric operands: " + left + ", " + right);
            return Type.ERROR;
        }

        return Type.max(left, right); // int + float -> float
    }

    @Override
    public Type visitRelational(pjplangParser.RelationalContext ctx) {
        Type left = visit(ctx.expression(0));
        Type right = visit(ctx.expression(1));

        if (!left.isNumeric() || !right.isNumeric()) {
            errors.add("Relational operator '" + ctx.op.getText() + "' used with non-numeric operands: " + left + ", " + right);
            return Type.ERROR;
        }

        return Type.BOOL;
    }

    @Override
    public Type visitCompare(pjplangParser.CompareContext ctx) {
        Type left = visit(ctx.expression(0));
        Type right = visit(ctx.expression(1));

        if (left != right) {
            errors.add("Comparison operator '" + ctx.op.getText() + "' used with different types: " + left + ", " + right);
            return Type.ERROR;
        }

        return Type.BOOL;
    }

    @Override
    public Type visitLogic(pjplangParser.LogicContext ctx) {
        Type left = visit(ctx.expression(0));
        Type right = visit(ctx.expression(1));

        if (left != Type.BOOL || right != Type.BOOL) {
            errors.add("Logical operator '" + ctx.op.getText() + "' used with non-bool operands: " + left + ", " + right);
            return Type.ERROR;
        }

        return Type.BOOL;
    }

    @Override
    public Type visitConcat(pjplangParser.ConcatContext ctx) {
        Type left = visit(ctx.expression(0));
        Type right = visit(ctx.expression(1));

        if (left != Type.STRING || right != Type.STRING) {
            errors.add("Concatenation (.) requires string operands, found: " + left + ", " + right);
            return Type.ERROR;
        }

        return Type.STRING;
    }
}

