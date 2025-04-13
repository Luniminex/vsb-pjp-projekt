package org.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Interpreter {

    private final Deque<Object> stack = new ArrayDeque<>();
    private final Map<String, Object> memory = new HashMap<>();
    private final Map<String, Integer> labels = new HashMap<>();
    private final List<String> instructions = new ArrayList<>();
    private final Queue<String> inputQueue;
    private int ip = 0;
    private boolean debug = false;

    public void enableDebug() {
        this.debug = true;
    }

    public Interpreter() {
        this.inputQueue = null;
    }

    public Interpreter(List<String> inputs) {
        this.inputQueue = new LinkedList<>(inputs);
    }

    public void execute(Path filePath) throws IOException {
        instructions.addAll(Files.readAllLines(filePath));

        // Label pass
        for (int i = 0; i < instructions.size(); i++) {
            String line = instructions.get(i).trim();
            if (line.startsWith("label ")) {
                String label = line.split("\\s+")[1].trim();
                labels.put(label, i);
            }
        }

        // Execution loop
        while (ip < instructions.size()) {
            String line = instructions.get(ip++).trim();
            if (line.isEmpty() || line.startsWith("//")) continue;

            debugPrint("Executing: " + line);

            String[] parts = line.split("\\s+", 3);
            String op = parts[0].trim();

            switch (op) {
                case "push" -> {
                    String type = parts[1];
                    String value = parts[2];
                    stack.push(switch (type) {
                        case "I" -> Integer.parseInt(value);
                        case "F" -> Double.parseDouble(value);
                        case "B" -> Boolean.parseBoolean(value);
                        case "S" -> value.substring(1, value.length() - 1); // strip quotes
                        default -> throw new RuntimeException("Unknown push type: " + type);
                    });
                }
                case "load" -> {
                    Object val = memory.get(parts[1]);
                    if (val == null) throw new RuntimeException("Variable not initialized: " + parts[1]);
                    stack.push(val);
                }
                case "save" -> {
                    Object val = stack.pop();
                    memory.put(parts[1], val);
                }
                case "add" -> binaryMath(parts[1], (a, b) -> a + b);
                case "sub" -> binaryMath(parts[1], (a, b) -> a - b);
                case "mul" -> binaryMath(parts[1], (a, b) -> a * b);
                case "div" -> binaryMath(parts[1], (a, b) -> a / b);
                case "mod" -> {
                    int b = (int) stack.pop();
                    int a = (int) stack.pop();
                    stack.push(a % b);
                }
                case "uminus" -> {
                    Object val = stack.pop();
                    if (val instanceof Integer i) stack.push(-i);
                    else if (val instanceof Double d) stack.push(-d);
                    else throw new RuntimeException("Invalid type for uminus: " + val);
                }
                case "concat" -> {
                    String b = (String) stack.pop();
                    String a = (String) stack.pop();
                    stack.push(a + b);
                }
                case "and" -> {
                    boolean b = (boolean) stack.pop();
                    boolean a = (boolean) stack.pop();
                    stack.push(a && b);
                }
                case "or" -> {
                    boolean b = (boolean) stack.pop();
                    boolean a = (boolean) stack.pop();
                    stack.push(a || b);
                }
                case "not" -> {
                    boolean val = (boolean) stack.pop();
                    stack.push(!val);
                }
                case "lt" -> compare(parts[1], (a, b) -> a < b);
                case "gt" -> compare(parts[1], (a, b) -> a > b);
                case "eq" -> {
                    Object b = stack.pop();
                    Object a = stack.pop();
                    stack.push(Objects.equals(a, b));
                }
                case "itof" -> {
                    int val = (int) stack.pop();
                    stack.push((double) val);
                }
                case "dup" -> stack.push(stack.peek());
                case "pop" -> stack.pop();
                case "print" -> {
                    int n = Integer.parseInt(parts[1]);
                    List<Object> values = new ArrayList<>();
                    for (int i = 0; i < n; i++) values.add(stack.pop());
                    Collections.reverse(values);
                    String output = values.stream().map(Object::toString).reduce((a, b) -> a + b).orElse("");
                    debugPrint("Output: " + output); // ← přidáno
                    System.out.println(output);
                }
                case "read" -> {
                    String type = parts[1];
                    String input = inputQueue != null ? Objects.requireNonNullElse(inputQueue.poll(), "") : new Scanner(System.in).nextLine();
                    stack.push(switch (type) {
                        case "I" -> Integer.parseInt(input);
                        case "F" -> Double.parseDouble(input);
                        case "B" -> Boolean.parseBoolean(input);
                        case "S" -> input;
                        default -> throw new RuntimeException("Unknown read type: " + type);
                    });
                }
                case "jmp" -> ip = labels.get(parts[1]);
                case "fjmp" -> {
                    boolean cond = (boolean) stack.pop();
                    if (!cond) ip = labels.get(parts[1]);
                }
                case "label" -> {} // skip
                default -> throw new RuntimeException("Unknown instruction: " + op);
            }
        }
    }

    private void binaryMath(String type, MathOp op) {
        Object right = stack.pop();
        Object left = stack.pop();

        if (type.equals("I")) {
            int a = (left instanceof Integer) ? (Integer) left : ((Double) left).intValue();
            int b = (right instanceof Integer) ? (Integer) right : ((Double) right).intValue();
            stack.push(op.apply(a, b));
        } else if (type.equals("F")) {
            double a = (left instanceof Integer) ? ((Integer) left).doubleValue() : (Double) left;
            double b = (right instanceof Integer) ? ((Integer) right).doubleValue() : (Double) right;
            stack.push(op.apply(a, b));
        } else {
            throw new RuntimeException("Unsupported math type: " + type);
        }
    }

    private void compare(String type, CompareOp op) {
        Object right = stack.pop();
        Object left = stack.pop();

        if (type.equals("I")) {
            int a = (left instanceof Integer) ? (Integer) left : ((Double) left).intValue();
            int b = (right instanceof Integer) ? (Integer) right : ((Double) right).intValue();
            stack.push(op.compare(a, b));
        } else if (type.equals("F")) {
            double a = (left instanceof Integer) ? ((Integer) left).doubleValue() : (Double) left;
            double b = (right instanceof Integer) ? ((Integer) right).doubleValue() : (Double) right;
            stack.push(op.compare(a, b));
        } else {
            throw new RuntimeException("Unsupported comparison type: " + type);
        }
    }

    private void debugPrint(String msg) {
        if (debug) {
            System.out.println("[DEBUG] " + msg);
        }
    }

    @FunctionalInterface
    private interface MathOp {
        double apply(double a, double b);
    }

    @FunctionalInterface
    private interface CompareOp {
        boolean compare(double a, double b);
    }
}
