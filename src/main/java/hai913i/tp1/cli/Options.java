package hai913i.tp1.cli;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Arguments validables sans toucher au disque. Le seuil vaut {@code null} tant qu'il
 * n'a pas ete fourni : c'est {@link Analyzer} qui exige sa presence, une fois le
 * projet verifie, pour que le chemin soit diagnostique avant le seuil.
 */
public record Options(Path projectDirectory,
                      Action action,
                      Integer threshold,
                      Optional<String> callees,
                      Optional<String> callers,
                      Optional<Path> dotFile,
                      Optional<String> dotPackage,
                      Optional<String> astFile,
                      boolean gui) {

    /** Vrai si le seuil X a ete fourni. */
    public boolean hasThreshold() {
        return threshold != null;
    }

    /** Le seuil, ou 0 s'il n'a pas ete fourni (actions qui ne s'en servent pas). */
    public int thresholdOrZero() {
        return threshold == null ? 0 : threshold;
    }

    /** Options minimales, pratiques pour les tests. */
    public static Options of(Path projectDirectory, Action action, int threshold) {
        return new Options(projectDirectory, action, threshold,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), false);
    }
}
