package hai913i.tp1.cli;

import java.nio.file.Path;
import java.util.Optional;

public record Options(Path projectDirectory,
                      Action action,
                      Integer threshold,
                      Optional<String> callees,
                      Optional<String> callers,
                      Optional<Path> dotFile,
                      Optional<String> dotPackage,
                      Optional<String> astFile,
                      boolean gui) {

    public boolean hasThreshold() {
        return threshold != null;
    }

    public int thresholdOrZero() {
        return threshold == null ? 0 : threshold;
    }

    public static Options of(Path projectDirectory, Action action, int threshold) {
        return new Options(projectDirectory, action, threshold,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), false);
    }
}
