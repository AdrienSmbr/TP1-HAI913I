package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

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
        return run(Options.of(Path.of(projectDirectory), action, 0));
    }

    private static Run run(Options options) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code;
        try (PrintStream outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
                PrintStream errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
            code = Analyzer.run(options, outStream, errStream);
        }
        return new Run(code,
                out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8));
    }

    /** Seuil absent : Options le porte a null, comme apres lecture de la ligne de commande. */
    private static Options withoutThreshold(String projectDirectory, Action action) {
        return new Options(Path.of(projectDirectory), action, null,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), false);
    }

    @Test
    @DisplayName("chemin inexistant : message clair, code de sortie non nul")
    void missingDirectory() {
        Run run = run(TestProjects.missing().toString(), Action.STRUCTURE);
        assertNotEquals(Analyzer.OK, run.code());
        assertTrue(run.err().contains("Erreur"));
        assertFalse(run.err().contains("Exception"), "pas de pile d'exception brute");
    }

    @Test
    @DisplayName("dossier sans fichier Java : message clair, code de sortie non nul")
    void noJavaFiles() {
        Run run = run(TestProjects.robustness("no-java-files").toString(), Action.STRUCTURE);
        assertEquals(Analyzer.NO_SOURCES, run.code());
        assertTrue(run.err().contains("aucun fichier .java"));
    }

    @Test
    @DisplayName("dependance manquante : appel non resolu signale, analyse complete")
    void missingDependency() {
        Run run = run(TestProjects.robustness("missing-dep").toString(), Action.CALLS);
        assertEquals(Analyzer.OK, run.code());
        assertTrue(run.out().contains("1 non resolus"),
                "StringUtils.capitalize n'a ni cible ni receveur");
        assertTrue(run.out().contains("demo.Formatter#decorate(java.lang.String)"),
                "l'appel a decorate, dans la meme methode, reste resolu");
    }

    @Test
    @DisplayName("X absent : le projet est verifie avant le seuil")
    void projectCheckedBeforeThreshold() {
        // Chemin invalide et seuil manquant : c'est le chemin qui doit etre signale.
        Run invalidPath = run(withoutThreshold(TestProjects.missing().toString(), Action.ALL));
        assertEquals(Analyzer.BAD_PROJECT, invalidPath.code());
        assertFalse(invalidPath.err().contains("seuil X"),
                "le seuil ne doit pas masquer un chemin invalide");

        // Dossier sans source et seuil manquant : c'est l'absence de .java qui prime.
        Run noSources = run(withoutThreshold(TestProjects.robustness("no-java-files").toString(), Action.ALL));
        assertEquals(Analyzer.NO_SOURCES, noSources.code());

        // Projet valide : le seuil manquant est alors le seul reproche.
        Run validProject = run(withoutThreshold(TestProjects.validation().toString(), Action.ALL));
        assertEquals(Analyzer.BAD_USAGE, validProject.code());
        assertTrue(validProject.err().contains("seuil X"));
    }

    @Test
    @DisplayName("erreur de syntaxe : erreurs signalees, l'autre fichier est analyse")
    void syntaxError() {
        Run run = run(TestProjects.robustness("syntax-error").toString(), Action.STRUCTURE);
        assertEquals(Analyzer.OK, run.code(), "l'outil ne s'arrete pas");
        assertTrue(run.err().contains("Broken.java"), "les erreurs de Broken.java sont signalees");
        assertTrue(run.out().contains("demo.Helper"), "Helper.java est analyse normalement");
    }
}
