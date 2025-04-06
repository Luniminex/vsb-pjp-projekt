package org.project;

public enum Type {
    INT, FLOAT, BOOL, STRING, ERROR;

    public static Type fromString(String s) {
        return switch (s) {
            case "int" -> INT;
            case "float" -> FLOAT;
            case "bool" -> BOOL;
            case "string" -> STRING;
            default -> ERROR;
        };
    }

    public boolean isNumeric() {
        return this == INT || this == FLOAT;
    }

    public static Type max(Type a, Type b) {
        if (a == FLOAT || b == FLOAT) return FLOAT;
        return INT;
    }
}
