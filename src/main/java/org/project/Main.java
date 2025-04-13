package org.project;

import grammar.pjplangLexer;
import grammar.pjplangParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main {

    private static final String SOURCE_FILES_DIR = "src/main/resources/";

    public static void main(String[] args) throws IOException {
        CharStream input = CharStreams.fromFileName(SOURCE_FILES_DIR + "test.pjp");
        pjplangLexer lexer = new pjplangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        pjplangParser parser = new pjplangParser(tokens);

        // volitelně – přidej kontrolu syntaxe
        SyntaxErrorCollector syntaxErrors = new SyntaxErrorCollector();
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxErrors);

        ParseTree tree = parser.program();

        if (syntaxErrors.hasErrors()) {
            System.out.println("Syntax error(s):");
            syntaxErrors.getErrors().forEach(System.out::println);
            System.exit(1);
        }

        TypeChecker checker = new TypeChecker();
        checker.visit(tree);

        if (checker.hasErrors()) {
            System.out.println("Type error(s) found:");
            checker.printErrors();
            System.exit(1);
        } else {
            System.out.println("No type errors found.");
        }
    }

    private static class SyntaxErrorCollector extends BaseErrorListener {
        private final java.util.List<String> errors = new java.util.ArrayList<>();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line, int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            errors.add("line " + line + ":" + charPositionInLine + " " + msg);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public java.util.List<String> getErrors() {
            return errors;
        }
    }
}
