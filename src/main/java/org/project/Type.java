package org.project;

// Type.java
public enum Type {
    INT, FLOAT, BOOL, STRING, ERROR;

    public boolean isNumeric() {
        return this == INT || this == FLOAT;
    }

    public static Type max(Type t1, Type t2) {
        if (t1 == ERROR || t2 == ERROR) return ERROR;
        if (t1 == FLOAT || t2 == FLOAT) return FLOAT;
        return INT;
    }
}
