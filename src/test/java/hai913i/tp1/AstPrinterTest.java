package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import hai913i.tp1.cli.Analyzer;
import hai913i.tp1.cli.CommandLine;

class AstPrinterTest {

    private static List<String> ast(String file) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8)) {
            int code = Analyzer.run(CommandLine.parse(new String[] {
                    "resources/validation", "--action", "ast", "--file", file}),
                    stream, stream);
            assertEquals(Analyzer.OK, code);
        }
        return Arrays.stream(out.toString(StandardCharsets.UTF_8).split("\\R"))
                .map(String::strip)
                .toList();
    }

    private static long count(List<String> nodes, String kind) {
        return nodes.stream().filter(kind::equals).count();
    }

    @Test
    @DisplayName("A1 : Dvd.java, un TypeDeclaration, un FieldDeclaration, trois MethodDeclaration")
    void dvd() throws Exception {
        List<String> nodes = ast("Dvd.java");
        assertEquals(1, count(nodes, "TypeDeclaration"));
        assertEquals(1, count(nodes, "FieldDeclaration"));
        assertEquals(3, count(nodes, "MethodDeclaration"));
        assertEquals(1, count(nodes, "SuperConstructorInvocation"));
    }

    @Test
    @DisplayName("A1 : Category.java a pour racine un EnumDeclaration, pas un TypeDeclaration")
    void category() throws Exception {
        List<String> nodes = ast("Category.java");
        assertEquals(1, count(nodes, "EnumDeclaration"));
        assertFalse(nodes.contains("TypeDeclaration"));
        assertTrue(nodes.contains("EnumConstantDeclaration"));
    }
}
