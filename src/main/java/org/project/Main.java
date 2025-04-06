package org.project;

import grammar.pjplangLexer;
import grammar.pjplangParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main {

    private static final String SOURCE_FILES_DIR = "src/main/resources//";
    public static void main(String[] args) throws IOException {
        CharStream input = CharStreams.fromFileName(SOURCE_FILES_DIR + "test.pjp");
        pjplangLexer lexer = new pjplangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        pjplangParser parser = new pjplangParser(tokens);

        ParseTree tree = parser.program();

        TypeChecker checker = new TypeChecker();
        checker.visit(tree);

        if (checker.hasErrors()) {
            checker.printErrors();
            System.exit(1);
        } else {
            System.out.println("No type errors found.");
        }
    }
}
