package hai913i.tp1.cli;

import java.nio.file.Path;
import java.util.Optional;

public record Options(Path projectDirectory,
                      Action action,
                      int threshold,
                      Optional<String> callees,
                      Optional<String> callers,
                      Optional<Path> dotFile,
                      Optional<String> dotPackage,
                      boolean gui) {

    /** Options minimales, pratiques pour les tests. */
    public static Options of(Path projectDirectory, Action action, int threshold) {
        return new Options(projectDirectory, action, threshold,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), false);
    }
}
