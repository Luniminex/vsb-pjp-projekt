package org.project;

import grammar.pjplangLexer;
import grammar.pjplangParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TypeCheckTest {

    private static final String TEST_PASS_DIR = "src/test/resources/tests/pass/";
    private static final String TEST_ERR_DIR = "src/test/resources/tests/errors/";

    public static Stream<File> providePassingFiles() {
        return getFilesFromDir(TEST_PASS_DIR);
    }

    public static Stream<File> provideFailingFiles() {
        return getFilesFromDir(TEST_ERR_DIR);
    }

    @ParameterizedTest
    @MethodSource("providePassingFiles")
    void testPassingFiles(File file) throws IOException {
        System.out.println("[TEST OK] " + file.getName());

        String content = Files.readString(file.toPath());
        CharStream input = CharStreams.fromString(content);
        pjplangLexer lexer = new pjplangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        pjplangParser parser = new pjplangParser(tokens);

        SyntaxErrorCollector syntaxErrors = new SyntaxErrorCollector();
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxErrors);

        ParseTree tree = parser.program();
        assertFalse(syntaxErrors.hasErrors(), "Unexpected syntax error in file: " + file.getName());

        TypeChecker checker = new TypeChecker();
        checker.visit(tree);
        assertFalse(checker.hasErrors(), "Unexpected type error in file: " + file.getName());
    }

    @ParameterizedTest
    @MethodSource("provideFailingFiles")
    void testFailingFiles(File file) throws IOException {
        System.out.println("[TEST ERR] " + file.getName());

        String content = Files.readString(file.toPath());
        CharStream input = CharStreams.fromString(content);
        pjplangLexer lexer = new pjplangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        pjplangParser parser = new pjplangParser(tokens);

        SyntaxErrorCollector syntaxErrors = new SyntaxErrorCollector();
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxErrors);

        ParseTree tree = parser.program();
        boolean hasSyntaxErrors = syntaxErrors.hasErrors();
        boolean hasTypeErrors = false;
        TypeChecker checker = null;

        if (!hasSyntaxErrors) {
            checker = new TypeChecker();
            checker.visit(tree);
            hasTypeErrors = checker.hasErrors();
        }

        assertTrue(hasSyntaxErrors || hasTypeErrors,
                "Expected error in file: " + file.getName() + ", but none occurred.");
    }

    private static Stream<File> getFilesFromDir(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException("Missing directory: " + dirPath);
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".pjp"));
        if (files == null || files.length == 0) {
            throw new RuntimeException("No test files found in: " + dirPath);
        }

        return Arrays.stream(files);
    }

    private static class SyntaxErrorCollector extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

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

        public List<String> getErrors() {
            return errors;
        }
    }
}
