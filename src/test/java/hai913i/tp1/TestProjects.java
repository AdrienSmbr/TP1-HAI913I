package hai913i.tp1;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Localise les projets d'essai du sujet, quel que soit l'endroit d'ou les tests
 * sont lances.
 *
 * <p>Les projets sont livres avec le depot, sous {@code resources/}, pour qu'un
 * {@code mvn clean test} reussisse sur une copie fraiche (point de controle B4).
 * Le repli sur {@code ../} sert au poste de developpement, ou ils sont voisins
 * du projet d'analyse comme dans l'arborescence du sujet.
 */
final class TestProjects {

    private TestProjects() {
    }

    /** Le projet de validation : 14 fichiers, 4 paquetages. */
    static Path validation() {
        return locate("validation");
    }

    /** Un echantillon de robustesse : missing-dep, syntax-error, no-java-files. */
    static Path robustness(String sample) {
        return locate(Path.of("robustness", sample).toString());
    }

    /** Un chemin qui n'existe pas, pour tester le diagnostic d'un projet absent. */
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
