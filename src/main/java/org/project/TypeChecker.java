package org.project;

import grammar.pjplangBaseVisitor;
import grammar.pjplangParser;

import java.util.*;

public class TypeChecker extends pjplangBaseVisitor<String> {

    private final Map<String, String> symbolTable = new HashMap<>();
    private final List<String> errors = new ArrayList<>();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void printErrors() {
        for (String err : errors) {
            System.err.println(err);
        }
    }

    // ======================
    // STATEMENTS
    // ======================

    @Override
    public String visitStatement(pjplangParser.StatementContext ctx) {
        // Deklarace proměnných
        if (ctx.type() != null && ctx.varList() != null) {
            String declaredType = ctx.type().getText();
            for (var id : ctx.varList().ID()) {
                String name = id.getText();
                if (symbolTable.containsKey(name)) {
                    errors.add("Variable already declared: " + name);
                } else {
                    symbolTable.put(name, declaredType);
                }
            }

            // Přiřazení
        } else if (ctx.ID() != null && ctx.expression() != null) {
            String varName = ctx.ID().getText();
            if (!symbolTable.containsKey(varName)) {
                errors.add("Undeclared variable: " + varName);
                return null;
            }

            String varType = symbolTable.get(varName);
            String exprType = visit(ctx.expression());

            if (exprType == null) return null;

            if (!varType.equals(exprType)) {
                if (!(varType.equals("float") && exprType.equals("int"))) {
                    errors.add("Cannot assign " + exprType + " to variable " + varName + " of type " + varType);
                }
            }

            // IF větev – poznáme podle toho, že obsahuje výraz a 1–2 statementy
        } else if (ctx.expression() != null && ctx.statement().size() >= 1 && ctx.getText().startsWith("if")) {
            String condType = visit(ctx.expression());
            if (!"bool".equals(condType)) {
                errors.add("Condition in 'if' must be bool, got: " + condType);
            }

            visit(ctx.statement(0));
            if (ctx.statement().size() > 1) {
                visit(ctx.statement(1));
            }

            // WHILE větev – poznáme podobně
        } else if (ctx.expression() != null && ctx.statement().size() == 1 && ctx.getText().startsWith("while")) {
            String condType = visit(ctx.expression());
            if (!"bool".equals(condType)) {
                errors.add("Condition in 'while' must be bool, got: " + condType);
            }

            visit(ctx.statement(0));

        } else {
            visitChildren(ctx);
        }

        return null;
    }



    // ======================
    // EXPRESSIONS
    // ======================

    @Override
    public String visitVarExpr(pjplangParser.VarExprContext ctx) {
        String name = ctx.ID().getText();
        if (!symbolTable.containsKey(name)) {
            errors.add("Undeclared variable used: " + name);
            return null;
        }
        return symbolTable.get(name);
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
        String type = visit(ctx.expression());
        if (type == null) return null;

        if (type.equals("int") || type.equals("float")) {
            return type;
        }

        errors.add("Unary minus is only applicable to int or float, not " + type);
        return null;
    }

    @Override
    public String visitNotExpr(pjplangParser.NotExprContext ctx) {
        String type = visit(ctx.expression());
        if (type == null) return null;

        if (!type.equals("bool")) {
            errors.add("Logical NOT (!) requires boolean, got: " + type);
            return null;
        }
        return "bool";
    }

    @Override
    public String visitAddSubConcat(pjplangParser.AddSubConcatContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));
        String op = ctx.op.getText();

        if (left == null || right == null) return null;

        if (op.equals(".")) {
            if (left.equals("string") && right.equals("string")) return "string";
        } else if (op.equals("+") || op.equals("-")) {
            if (left.equals("int") && right.equals("int")) return "int";
            if ((left.equals("float") && right.equals("int")) || (left.equals("int") && right.equals("float")) || (left.equals("float") && right.equals("float"))) {
                return "float";
            }
        }

        errors.add("Operator '" + op + "' not valid for types: " + left + ", " + right);
        return null;
    }

    @Override
    public String visitMulDivMod(pjplangParser.MulDivModContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));
        String op = ctx.op.getText();

        if (left == null || right == null) return null;

        if (op.equals("%")) {
            if (left.equals("int") && right.equals("int")) return "int";
            errors.add("Modulo requires two integers");
            return null;
        }

        if (left.equals("int") && right.equals("int")) return "int";
        if ((left.equals("float") && right.equals("int")) || (left.equals("int") && right.equals("float")) || (left.equals("float") && right.equals("float"))) {
            return "float";
        }

        errors.add("Invalid operand types for '" + op + "': " + left + ", " + right);
        return null;
    }

    @Override
    public String visitRelational(pjplangParser.RelationalContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));

        if (left == null || right == null) return null;

        if ((left.equals("int") || left.equals("float")) && (right.equals("int") || right.equals("float"))) {
            return "bool";
        }

        errors.add("Relational operators require int or float, got: " + left + ", " + right);
        return null;
    }

    @Override
    public String visitEquality(pjplangParser.EqualityContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));

        if (left == null || right == null) return null;

        if (left.equals(right)) return "bool";

        // int == float and float == int is allowed
        if ((left.equals("int") && right.equals("float")) || (left.equals("float") && right.equals("int"))) {
            return "bool";
        }

        errors.add("Equality check requires compatible types, got: " + left + ", " + right);
        return null;
    }

    @Override
    public String visitAndExpr(pjplangParser.AndExprContext ctx) {
        return checkLogicalBinary(ctx.expression(0), ctx.expression(1), "&&");
    }

    @Override
    public String visitOrExpr(pjplangParser.OrExprContext ctx) {
        return checkLogicalBinary(ctx.expression(0), ctx.expression(1), "||");
    }

    private String checkLogicalBinary(pjplangParser.ExpressionContext leftCtx, pjplangParser.ExpressionContext rightCtx, String op) {
        String left = visit(leftCtx);
        String right = visit(rightCtx);

        if (left == null || right == null) return null;

        if (left.equals("bool") && right.equals("bool")) return "bool";

        errors.add("Operator '" + op + "' requires bool operands, got: " + left + ", " + right);
        return null;
    }

    @Override
    public String visitTernaryExpr(pjplangParser.TernaryExprContext ctx) {
        String cond = visit(ctx.expression(0));
        String thenExpr = visit(ctx.expression(1));
        String elseExpr = visit(ctx.expression(2));

        if (cond == null || thenExpr == null || elseExpr == null) return null;

        if (!cond.equals("bool")) {
            errors.add("Condition in ternary expression must be bool, got: " + cond);
        }

        if (thenExpr.equals(elseExpr)) return thenExpr;

        // implicit int -> float
        if ((thenExpr.equals("int") && elseExpr.equals("float")) || (thenExpr.equals("float") && elseExpr.equals("int"))) {
            return "float";
        }

        errors.add("Ternary branches must be of compatible types, got: " + thenExpr + ", " + elseExpr);
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

