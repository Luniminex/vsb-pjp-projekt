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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CodeGenTest {

    private static final String TEST_DIR = "src/test/resources/tests/";

    public static Stream<File> providePassingFiles() {
        return getFilesWithPrefix("ok_");
    }

    @ParameterizedTest
    @MethodSource("providePassingFiles")
    void testCodeGeneration(File file) throws IOException {
        String content = Files.readString(file.toPath());
        CharStream input = CharStreams.fromString(content);
        pjplangLexer lexer = new pjplangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        pjplangParser parser = new pjplangParser(tokens);

        SyntaxErrorCollector syntaxErrors = new SyntaxErrorCollector();
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxErrors);

        ParseTree tree = parser.program();
        assertFalse(syntaxErrors.hasErrors(), "Syntax error(s) in file: " + file.getName());

        TypeChecker checker = new TypeChecker();
        checker.visit(tree);
        assertFalse(checker.hasErrors(), "Type error(s) in file: " + file.getName());

        CodeGenerator generator = new CodeGenerator();
        generator.visit(tree);
        List<String> code = generator.getInstructions();

        String actual = normalizeLabels(code);

        // 📦 ZGENERUJ výstupní .code soubor
        String generatedPath = file.getPath().replace("ok_", "generated_").replace(".pjp", ".code");
        Files.writeString(new File(generatedPath).toPath(), actual);

        // 📥 Načti očekávaný .code soubor
        String expectedPath = file.getPath().replace("ok_", "expected_").replace(".pjp", ".code");
        File expectedFile = new File(expectedPath);
        assertTrue(expectedFile.exists(), "Expected file missing: " + expectedFile.getName());

        String expectedRaw = Files.readString(expectedFile.toPath()).trim();
        String expected = normalizeLabels(Arrays.asList(expectedRaw.split("\n")));

        assertEquals(expected, actual, "Generated code doesn't match expected for: " + file.getName());
    }


    private static String normalizeLabels(List<String> lines) {
        Map<String, String> labelMap = new HashMap<>();
        AtomicInteger counter = new AtomicInteger();

        Pattern labelPattern = Pattern.compile("^(label|jmp|fjmp)\\s+(L\\d+)$");
        List<String> normalized = new ArrayList<>();

        for (String line : lines) {
            Matcher m = labelPattern.matcher(line.trim());
            if (m.matches()) {
                String cmd = m.group(1);
                String oldLabel = m.group(2);
                String newLabel = labelMap.computeIfAbsent(oldLabel, k -> "L" + (counter.getAndIncrement()));
                normalized.add(cmd + " " + newLabel);
            } else {
                normalized.add(line.trim());
            }
        }

        return String.join("\n", normalized).trim();
    }

    private static Stream<File> getFilesWithPrefix(String prefix) {
        File dir = new File(TEST_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException("Missing test directory: " + TEST_DIR);
        }

        File[] files = dir.listFiles((d, name) -> name.startsWith(prefix) && name.endsWith(".pjp"));
        if (files == null || files.length == 0) {
            throw new RuntimeException("No test files found with prefix: " + prefix);
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
