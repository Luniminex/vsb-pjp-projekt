package org.project;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpreterTest {

    private static final String TEST_DIR = "src/test/resources/tests/";
    private static final String GENERATED_DIR = TEST_DIR + "generated/";

    @BeforeAll
    static void generateAll() throws IOException {
        new CodeGenTest().generateAll();
    }

    @Test
    void testOk1() throws IOException {
        runAndLogOutput("generated_ok_1.code", false);
    }

    @Test
    void testOk2() throws IOException {
        runAndLogOutput("generated_ok_2.code", false);
    }

    @Test
    void testOk3() throws IOException {
        runAndLogOutput("generated_ok_3.code", true);
    }

    @Test
    void testOk4() throws IOException {
        runAndLogOutput("generated_ok_4.code", false);
    }

    @Test
    void testOk5WithSimulatedInput() throws IOException {
        Path codePath = Path.of(GENERATED_DIR + "generated_ok_5.code");

        // vstupní hodnoty pro read: int, float, string, bool
        List<String> simulatedInput = List.of("42", "3.14", "hello", "true");

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent)); // zachytit výstup

        Interpreter interpreter = new Interpreter(simulatedInput);
        interpreter.enableDebug(); // zapnout debug log
        interpreter.execute(codePath);

        System.setOut(originalOut); // reset
        String output = outContent.toString();
        System.out.println("==== OUTPUT (ok_5) ====");
        System.out.println(output);
        System.out.println("==== END ====");

        assertTrue(output.contains("42"), "Output should contain the read int value");
        assertTrue(output.contains("hello"), "Output should contain the read string value");
        assertTrue(output.contains("true"), "Output should contain the read bool value");
    }

    private void runAndLogOutput(String fileName, boolean assertNotEmpty) throws IOException {
        Path codePath = Path.of(GENERATED_DIR + fileName);

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        Interpreter interpreter = new Interpreter();
        interpreter.enableDebug();
        interpreter.execute(codePath);

        System.setOut(originalOut);
        String output = outContent.toString();

        System.out.println("==== OUTPUT (" + fileName + ") ====");
        System.out.println(output);
        System.out.println("==== END ====");

        if (assertNotEmpty) {
            assertTrue(!output.trim().isEmpty(), "Output should not be empty for: " + fileName);
        }
    }
}
