package org.project;

import grammar.pjplangBaseVisitor;
import grammar.pjplangParser;

import java.util.Map;

public class ExpressionTypeResolver extends pjplangBaseVisitor<String> {

    private final Map<String, String> symbolTable;

    public ExpressionTypeResolver(Map<String, String> symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    public String visitVarExpr(pjplangParser.VarExprContext ctx) {
        return symbolTable.get(ctx.ID().getText());
    }

    @Override
    public String visitIntLit(pjplangParser.IntLitContext ctx) {
        return "int";
    }

    @Override
    public String visitFloatLit(pjplangParser.FloatLitContext ctx) {
        return "float";
    }

    @Override
    public String visitBoolLit(pjplangParser.BoolLitContext ctx) {
        return "bool";
    }

    @Override
    public String visitStringLit(pjplangParser.StringLitContext ctx) {
        return "string";
    }

    @Override
    public String visitUnaryMinus(pjplangParser.UnaryMinusContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public String visitNotExpr(pjplangParser.NotExprContext ctx) {
        return "bool";
    }

    @Override
    public String visitAddSubConcat(pjplangParser.AddSubConcatContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));
        String op = ctx.op.getText();

        if (op.equals(".")) {
            return "string";
        } else if (op.equals("+") || op.equals("-")) {
            if (left.equals("float") || right.equals("float")) return "float";
            return "int";
        }
        return null;
    }

    @Override
    public String visitMulDivMod(pjplangParser.MulDivModContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));
        String op = ctx.op.getText();

        if (op.equals("%")) return "int";
        if (left.equals("float") || right.equals("float")) return "float";
        return "int";
    }

    @Override
    public String visitRelational(pjplangParser.RelationalContext ctx) {
        return "bool";
    }

    @Override
    public String visitEquality(pjplangParser.EqualityContext ctx) {
        return "bool";
    }

    @Override
    public String visitAndExpr(pjplangParser.AndExprContext ctx) {
        return "bool";
    }

    @Override
    public String visitOrExpr(pjplangParser.OrExprContext ctx) {
        return "bool";
    }

    @Override
    public String visitTernaryExpr(pjplangParser.TernaryExprContext ctx) {
        String thenType = visit(ctx.expression(1));
        String elseType = visit(ctx.expression(2));
        if (thenType.equals(elseType)) return thenType;

        // implicit int -> float
        if ((thenType.equals("int") && elseType.equals("float")) ||
                (thenType.equals("float") && elseType.equals("int"))) {
            return "float";
        }

        return null;
    }

    @Override
    public String visitParenExpr(pjplangParser.ParenExprContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    protected String defaultResult() {
        return null;
    }
}
