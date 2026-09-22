package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import hai913i.tp1.cli.Action;
import hai913i.tp1.cli.Analyzer;
import hai913i.tp1.cli.Options;

/** Point de controle B4 : les trois echantillons de robustesse. */
class RobustnessTest {

    private record Run(int code, String out, String err) {
    }

    private static Run run(String projectDirectory, Action action) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code;
        try (PrintStream outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
                PrintStream errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
            code = Analyzer.run(Options.of(Path.of(projectDirectory), action, 0),
                    outStream, errStream);
        }
        return new Run(code,
                out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("chemin inexistant : message clair, code de sortie non nul")
    void missingDirectory() {
        Run run = run("../aucun-projet-ici", Action.STRUCTURE);
        assertNotEquals(Analyzer.OK, run.code());
        assertTrue(run.err().contains("Erreur"));
        assertFalse(run.err().contains("Exception"), "pas de pile d'exception brute");
    }

    @Test
    @DisplayName("dossier sans fichier Java : message clair, code de sortie non nul")
    void noJavaFiles() {
        Run run = run("../robustness/no-java-files", Action.STRUCTURE);
        assertEquals(Analyzer.NO_SOURCES, run.code());
        assertTrue(run.err().contains("aucun fichier .java"));
    }

    @Test
    @DisplayName("dependance manquante : appel non resolu signale, analyse complete")
    void missingDependency() {
        Run run = run("../robustness/missing-dep", Action.CALLS);
        assertEquals(Analyzer.OK, run.code());
        assertTrue(run.out().contains("1 non resolus"),
                "StringUtils.capitalize n'a ni cible ni receveur");
        assertTrue(run.out().contains("demo.Formatter#decorate(java.lang.String)"),
                "l'appel a decorate, dans la meme methode, reste resolu");
    }

    @Test
    @DisplayName("erreur de syntaxe : erreurs signalees, l'autre fichier est analyse")
    void syntaxError() {
        Run run = run("../robustness/syntax-error", Action.STRUCTURE);
        assertEquals(Analyzer.OK, run.code(), "l'outil ne s'arrete pas");
        assertTrue(run.err().contains("Broken.java"), "les erreurs de Broken.java sont signalees");
        assertTrue(run.out().contains("demo.Helper"), "Helper.java est analyse normalement");
    }
}
