package org.project;

import grammar.pjplangBaseVisitor;
import grammar.pjplangParser;

import java.util.*;

public class CodeGenerator extends pjplangBaseVisitor<Void> {

    private final List<String> instructions = new ArrayList<>();
    private final Map<String, String> symbolTable = new HashMap<>();
    private int labelCounter = 0;

    public List<String> getInstructions() {
        return instructions;
    }

    private String newLabel() {
        return "L" + (labelCounter++);
    }

    private String getExpressionType(pjplangParser.ExpressionContext ctx) {
        ExpressionTypeResolver resolver = new ExpressionTypeResolver(symbolTable);
        return resolver.visit(ctx);
    }

    @Override
    public Void visitStatement(pjplangParser.StatementContext ctx) {
        if (ctx.type() != null && ctx.varList() != null) {
            String type = ctx.type().getText();
            for (var id : ctx.varList().ID()) {
                String name = id.getText();
                symbolTable.put(name, type);
                switch (type) {
                    case "int" -> instructions.add("push I 0");
                    case "float" -> instructions.add("push F 0.0");
                    case "bool" -> instructions.add("push B false");
                    case "string" -> instructions.add("push S \"\"");
                }
                instructions.add("save " + name);
            }

        } else if (ctx.ID() != null && ctx.expression() != null) {
            String var = ctx.ID().getText();
            String varType = symbolTable.get(var);
            String exprType = getExpressionType(ctx.expression());

            visit(ctx.expression());

            if ("int".equals(exprType) && "float".equals(varType)) {
                instructions.add("itof");
            }

            instructions.add("save " + var);
            instructions.add("load " + var);
            instructions.add("pop");

        } else if (ctx.getText().startsWith("write")) {
            var exprs = ctx.exprList().expression();
            for (var e : exprs) visit(e);
            instructions.add("print " + exprs.size());

        } else if (ctx.getText().startsWith("read")) {
            for (var id : ctx.varList().ID()) {
                String name = id.getText();
                String type = symbolTable.get(name);
                String t = switch (type) {
                    case "int" -> "I";
                    case "float" -> "F";
                    case "bool" -> "B";
                    case "string" -> "S";
                    default -> throw new RuntimeException("Unknown type: " + type);
                };
                instructions.add("read " + t);
                instructions.add("save " + name);
            }

        } else if (ctx.getText().startsWith("if")) {
            String elseLabel = newLabel();
            String endLabel = newLabel();

            visit(ctx.expression());
            instructions.add("fjmp " + elseLabel);
            visit(ctx.statement(0));
            if (ctx.statement().size() > 1) {
                instructions.add("jmp " + endLabel);
                instructions.add("label " + elseLabel);
                visit(ctx.statement(1));
                instructions.add("label " + endLabel);
            } else {
                instructions.add("label " + elseLabel);
            }

        } else if (ctx.getText().startsWith("while")) {
            String labelStart = newLabel();
            String labelCond = newLabel();
            String labelEnd = newLabel();

            instructions.add("label " + labelCond);
            visit(ctx.expression());           // evaluate condition
            instructions.add("fjmp " + labelEnd); // if false → jump out
            instructions.add("label " + labelStart);
            visit(ctx.statement(0));           // loop body
            instructions.add("jmp " + labelCond); // back to condition
            instructions.add("label " + labelEnd);
        }

        else if (ctx.expression() != null) {
            visit(ctx.expression());
            instructions.add("pop");

        } else {
            visitChildren(ctx);
        }

        return null;
    }

    @Override public Void visitIntLit(pjplangParser.IntLitContext ctx) { instructions.add("push I " + ctx.getText()); return null; }
    @Override public Void visitFloatLit(pjplangParser.FloatLitContext ctx) { instructions.add("push F " + ctx.getText()); return null; }
    @Override public Void visitBoolLit(pjplangParser.BoolLitContext ctx) { instructions.add("push B " + ctx.getText()); return null; }
    @Override public Void visitStringLit(pjplangParser.StringLitContext ctx) { instructions.add("push S " + ctx.getText()); return null; }
    @Override public Void visitVarExpr(pjplangParser.VarExprContext ctx) { instructions.add("load " + ctx.ID().getText()); return null; }

    @Override
    public Void visitUnaryMinus(pjplangParser.UnaryMinusContext ctx) {
        String type = getExpressionType(ctx.expression());
        visit(ctx.expression());
        instructions.add("uminus " + ("float".equals(type) ? "F" : "I"));
        return null;
    }

    @Override
    public Void visitNotExpr(pjplangParser.NotExprContext ctx) {
        visit(ctx.expression());
        instructions.add("not");
        return null;
    }

    @Override
    public Void visitAddSubConcat(pjplangParser.AddSubConcatContext ctx) {
        handleBinary(ctx, ctx.op.getText());
        return null;
    }

    @Override
    public Void visitMulDivMod(pjplangParser.MulDivModContext ctx) {
        handleBinary(ctx, ctx.op.getText());
        return null;
    }

    @Override
    public Void visitEquality(pjplangParser.EqualityContext ctx) {
        handleBinary(ctx, "eq");
        if (ctx.op.getText().equals("!=")) instructions.add("not");
        return null;
    }

    @Override
    public Void visitRelational(pjplangParser.RelationalContext ctx) {
        handleBinary(ctx, ctx.op.getText().equals("<") ? "lt" : "gt");
        return null;
    }

    @Override
    public Void visitAndExpr(pjplangParser.AndExprContext ctx) {
        visit(ctx.expression(0)); visit(ctx.expression(1));
        instructions.add("and");
        return null;
    }

    @Override
    public Void visitOrExpr(pjplangParser.OrExprContext ctx) {
        visit(ctx.expression(0)); visit(ctx.expression(1));
        instructions.add("or");
        return null;
    }

    @Override
    public Void visitTernaryExpr(pjplangParser.TernaryExprContext ctx) {
        String labelFalse = newLabel();
        String labelEnd = newLabel();
        visit(ctx.expression(0));
        instructions.add("fjmp " + labelFalse);
        visit(ctx.expression(1));
        instructions.add("jmp " + labelEnd);
        instructions.add("label " + labelFalse);
        visit(ctx.expression(2));
        instructions.add("label " + labelEnd);
        return null;
    }

    @Override
    public Void visitParenExpr(pjplangParser.ParenExprContext ctx) {
        visit(ctx.expression());
        return null;
    }

    private void handleBinary(pjplangParser.ExpressionContext ctx, String op) {
        var left = ctx.getChild(0);
        var right = ctx.getChild(2);
        String lType = getExpressionType((pjplangParser.ExpressionContext) left);
        String rType = getExpressionType((pjplangParser.ExpressionContext) right);

        visit((pjplangParser.ExpressionContext) left);
        if ("int".equals(lType) && "float".equals(rType)) instructions.add("itof");

        visit((pjplangParser.ExpressionContext) right);
        if ("float".equals(lType) && "int".equals(rType)) instructions.add("itof");

        switch (op) {
            case "+" -> instructions.add("add " + resolveMathType(lType, rType));
            case "-" -> instructions.add("sub " + resolveMathType(lType, rType));
            case "*" -> instructions.add("mul " + resolveMathType(lType, rType));
            case "/" -> instructions.add("div " + resolveMathType(lType, rType));
            case "%" -> instructions.add("mod");
            case "." -> instructions.add("concat");
            case "eq" -> instructions.add("eq " + resolveComparisonType(lType, rType));
            case "lt" -> instructions.add("lt " + resolveMathType(lType, rType));
            case "gt" -> instructions.add("gt " + resolveMathType(lType, rType));
        }
    }

    private String resolveMathType(String left, String right) {
        return ("float".equals(left) || "float".equals(right)) ? "F" : "I";
    }

    private String resolveComparisonType(String left, String right) {
        if ("string".equals(left) || "string".equals(right)) return "S";
        if ("float".equals(left) || "float".equals(right)) return "F";
        return "I";
    }
}
