package org.project;

import grammar.pjplangBaseVisitor;
import grammar.pjplangParser;

import java.util.*;

public class CodeGenerator extends pjplangBaseVisitor<Void> {

    private final List<String> instructions = new ArrayList<>();
    private final Map<String, String> symbolTable = new HashMap<>(); // var name → type
    private int labelCounter = 0;

    public List<String> getInstructions() {
        return instructions;
    }

    private String newLabel() {
        return "L" + (labelCounter++);
    }

    // =======================
    // Statements
    // =======================

    @Override
    public Void visitStatement(pjplangParser.StatementContext ctx) {
        if (ctx.type() != null && ctx.varList() != null) {
            String type = ctx.type().getText();
            for (var id : ctx.varList().ID()) {
                symbolTable.put(id.getText(), type);
                // žádná instr. — defaultní hodnota se nastaví jen v interpretéru
            }

        } else if (ctx.ID() != null && ctx.expression() != null) {
            String varName = ctx.ID().getText();
            String varType = symbolTable.get(varName);
            String exprType = visitAndGetType(ctx.expression());

            if ("int".equals(exprType) && "float".equals(varType)) {
                instructions.add("itof");
            }

            // výrazy přidají push/load operace
            visit(ctx.expression());
            instructions.add("save " + varName);

        } else if (ctx.getText().startsWith("write")) {
            List<pjplangParser.ExpressionContext> exprs = ctx.exprList().expression();
            for (pjplangParser.ExpressionContext expr : exprs) {
                visit(expr);
            }
            instructions.add("print " + exprs.size());

        } else if (ctx.getText().startsWith("read")) {
            for (var id : ctx.varList().ID()) {
                String name = id.getText();
                String type = symbolTable.get(name);
                String codeType = switch (type) {
                    case "int" -> "I";
                    case "float" -> "F";
                    case "bool" -> "B";
                    case "string" -> "S";
                    default -> throw new RuntimeException("Unknown type: " + type);
                };
                instructions.add("read " + codeType);
                instructions.add("save " + name);
            }

        } else if (ctx.getText().startsWith("if")) {
            String labelElse = newLabel();
            String labelEnd = newLabel();

            visit(ctx.expression());
            instructions.add("fjmp " + labelElse);
            visit(ctx.statement(0));
            instructions.add("jmp " + labelEnd);
            instructions.add("label " + labelElse);
            if (ctx.statement().size() > 1) {
                visit(ctx.statement(1));
            }
            instructions.add("label " + labelEnd);

        } else if (ctx.getText().startsWith("while")) {
            String labelStart = newLabel();
            String labelEnd = newLabel();

            instructions.add("label " + labelStart);
            visit(ctx.expression());
            instructions.add("fjmp " + labelEnd);
            visit(ctx.statement(0));
            instructions.add("jmp " + labelStart);
            instructions.add("label " + labelEnd);

        } else {
            visitChildren(ctx);
        }

        return null;
    }

    // =======================
    // Expressions
    // =======================

    private String visitAndGetType(pjplangParser.ExpressionContext ctx) {
        ExpressionTypeResolver resolver = new ExpressionTypeResolver(symbolTable);
        return resolver.visit(ctx);
    }

    @Override
    public Void visitVarExpr(pjplangParser.VarExprContext ctx) {
        instructions.add("load " + ctx.ID().getText());
        return null;
    }

    @Override
    public Void visitIntLit(pjplangParser.IntLitContext ctx) {
        instructions.add("push I " + ctx.getText());
        return null;
    }

    @Override
    public Void visitFloatLit(pjplangParser.FloatLitContext ctx) {
        instructions.add("push F " + ctx.getText());
        return null;
    }

    @Override
    public Void visitBoolLit(pjplangParser.BoolLitContext ctx) {
        instructions.add("push B " + ctx.getText());
        return null;
    }

    @Override
    public Void visitStringLit(pjplangParser.StringLitContext ctx) {
        instructions.add("push S " + ctx.getText());
        return null;
    }

    @Override
    public Void visitUnaryMinus(pjplangParser.UnaryMinusContext ctx) {
        String type = visitAndGetType(ctx.expression());
        visit(ctx.expression());
        instructions.add("uminus " + (type.equals("float") ? "F" : "I"));
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
        String leftType = visitAndGetType(ctx.expression(0));
        String rightType = visitAndGetType(ctx.expression(1));
        visit(ctx.expression(0));
        visit(ctx.expression(1));

        String op = ctx.op.getText();
        switch (op) {
            case "." -> instructions.add("concat");
            case "+" -> instructions.add("add " + resolveMathType(leftType, rightType));
            case "-" -> instructions.add("sub " + resolveMathType(leftType, rightType));
        }
        return null;
    }

    @Override
    public Void visitMulDivMod(pjplangParser.MulDivModContext ctx) {
        String leftType = visitAndGetType(ctx.expression(0));
        String rightType = visitAndGetType(ctx.expression(1));
        visit(ctx.expression(0));
        visit(ctx.expression(1));

        String op = ctx.op.getText();
        switch (op) {
            case "*" -> instructions.add("mul " + resolveMathType(leftType, rightType));
            case "/" -> instructions.add("div " + resolveMathType(leftType, rightType));
            case "%" -> instructions.add("mod");
        }
        return null;
    }

    @Override
    public Void visitEquality(pjplangParser.EqualityContext ctx) {
        String type = visitAndGetType(ctx.expression(0));
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        instructions.add("eq " + resolveComparisonType(type));
        return null;
    }

    @Override
    public Void visitRelational(pjplangParser.RelationalContext ctx) {
        String type = visitAndGetType(ctx.expression(0));
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        if (ctx.op.getText().equals("<")) {
            instructions.add("lt " + resolveMathType(type, type));
        } else {
            instructions.add("gt " + resolveMathType(type, type));
        }
        return null;
    }

    @Override
    public Void visitAndExpr(pjplangParser.AndExprContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        instructions.add("and");
        return null;
    }

    @Override
    public Void visitOrExpr(pjplangParser.OrExprContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        instructions.add("or");
        return null;
    }

    @Override
    public Void visitTernaryExpr(pjplangParser.TernaryExprContext ctx) {
        String elseLabel = newLabel();
        String endLabel = newLabel();

        visit(ctx.expression(0)); // condition
        instructions.add("fjmp " + elseLabel);
        visit(ctx.expression(1)); // if true
        instructions.add("jmp " + endLabel);
        instructions.add("label " + elseLabel);
        visit(ctx.expression(2)); // if false
        instructions.add("label " + endLabel);

        return null;
    }

    @Override
    public Void visitParenExpr(pjplangParser.ParenExprContext ctx) {
        visit(ctx.expression());
        return null;
    }

    private String resolveMathType(String left, String right) {
        if (left.equals("float") || right.equals("float")) return "F";
        return "I";
    }

    private String resolveComparisonType(String type) {
        return switch (type) {
            case "int" -> "I";
            case "float" -> "F";
            case "string" -> "S";
            default -> throw new RuntimeException("Unsupported comparison type: " + type);
        };
    }
}
