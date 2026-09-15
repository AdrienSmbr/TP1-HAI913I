package hai913i.tp1.parse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public record ProjectSources(Path projectDirectory, Path sourceRoot, List<Path> javaFiles) {

    private static final Set<String> IGNORED_DIRECTORIES = Set.of("target", "build", "out", "bin");

    private static final List<String> CANDIDATE_ROOTS = List.of("src/main/java", "src");

    public static ProjectSources of(Path projectDirectory) throws IOException {
        if (projectDirectory == null || !Files.isDirectory(projectDirectory)) {
            throw new IllegalArgumentException("Pas un repertoire : " + projectDirectory);
        }
        Path project = projectDirectory.toAbsolutePath().normalize();
        Path root = findSourceRoot(project);
        return new ProjectSources(project, root, collectJavaFiles(root));
    }

    private static Path findSourceRoot(Path project) {
        return CANDIDATE_ROOTS.stream()
                .map(project::resolve)
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(project);
    }

    private static List<Path> collectJavaFiles(Path root) throws IOException {
        // try-with-resources : Files.walk ouvre des descripteurs qu'il faut refermer.
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !isInIgnoredDirectory(root, path))
                    .sorted()
                    .toList();
        }
    }

    private static boolean isInIgnoredDirectory(Path root, Path file) {
        for (Path segment : root.relativize(file.getParent())) {
            String name = segment.toString();
            if (IGNORED_DIRECTORIES.contains(name) || name.startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return javaFiles.isEmpty();
    }
}
