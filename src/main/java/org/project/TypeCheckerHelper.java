package org.project;

public class TypeCheckerHelper {
    public static boolean isAssignable(Type varType, Type valueType) {
        if (varType == valueType) return true;
        return varType == Type.FLOAT && valueType == Type.INT;
    }
}
