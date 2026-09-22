package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import hai913i.tp1.cli.Action;
import hai913i.tp1.cli.CommandLine;
import hai913i.tp1.cli.Options;
import hai913i.tp1.cli.UsageException;

/** Point de controle B4 : la ligne de commande refuse proprement les entrees invalides. */
class CommandLineTest {

    @Test
    @DisplayName("chemin manquant")
    void missingPath() {
        assertThrows(UsageException.class, () -> CommandLine.parse(new String[] {}));
    }

    @Test
    @DisplayName("X = abc : refus explicite")
    void malformedThreshold() {
        UsageException e = assertThrows(UsageException.class,
                () -> CommandLine.parse(new String[] {"p", "--x", "abc"}));
        assertTrue(e.getMessage().contains("abc"), "le message doit citer la valeur fautive");
    }

    @Test
    @DisplayName("X negatif : refus explicite")
    void negativeThreshold() {
        assertThrows(UsageException.class,
                () -> CommandLine.parse(new String[] {"p", "--x", "-1"}));
    }

    @Test
    @DisplayName("X absent : la lecture des arguments ne tranche pas, Analyzer le fera")
    void missingThreshold() throws UsageException {
        // Le refus a lieu dans Analyzer, apres verification du projet, pour qu'un
        // chemin invalide soit diagnostique avant le seuil (voir RobustnessTest).
        assertFalse(CommandLine.parse(new String[] {"p", "--action", "metrics"}).hasThreshold());
        assertFalse(CommandLine.parse(new String[] {"p"}).hasThreshold());
    }

    @Test
    @DisplayName("X = 0 est une valeur valide")
    void zeroThreshold() throws UsageException {
        assertEquals(0, CommandLine.parse(new String[] {"p", "--x", "0"}).threshold());
    }

    @Test
    @DisplayName("action inconnue, option inconnue, valeur manquante")
    void unknownArguments() {
        assertThrows(UsageException.class,
                () -> CommandLine.parse(new String[] {"p", "--action", "toto", "--x", "1"}));
        assertThrows(UsageException.class,
                () -> CommandLine.parse(new String[] {"p", "--inconnue"}));
        assertThrows(UsageException.class, () -> CommandLine.parse(new String[] {"p", "--x"}));
    }

    @Test
    @DisplayName("une action sans seuil n'exige pas X")
    void graphNeedsNoThreshold() throws UsageException {
        Options options = CommandLine.parse(new String[] {"p", "--action", "graph"});
        assertEquals(Action.GRAPH, options.action());
    }
}
