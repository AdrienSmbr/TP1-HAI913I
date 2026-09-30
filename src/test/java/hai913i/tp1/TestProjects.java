package hai913i.tp1;

import java.nio.file.Files;
import java.nio.file.Path;

final class TestProjects {

    private TestProjects() {
    }

    static Path validation() {
        return locate("validation");
    }

    static Path robustness(String sample) {
        return locate(Path.of("robustness", sample).toString());
    }

    static Path missing() {
        return Path.of("..", "aucun-projet-ici");
    }

    private static Path locate(String name) {
        Path bundled = Path.of("resources").resolve(name);
        if (Files.isDirectory(bundled)) {
            return bundled;
        }
        Path sibling = Path.of("..").resolve(name);
        if (Files.isDirectory(sibling)) {
            return sibling;
        }
        throw new IllegalStateException("projet d'essai introuvable : " + name
                + " (cherche dans resources/ puis ../)");
    }
}
