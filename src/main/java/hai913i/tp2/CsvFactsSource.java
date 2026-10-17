package hai913i.tp2;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Chemin 2 du sujet : lecture et validation du format canonique fourni. */
public record CsvFactsSource(Path directory, String prefix) implements FactsSource {
    public static String discoverPrefix(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) throw new IOException("dossier introuvable : " + dir);
        List<String> found;
        try (var files = Files.list(dir)) {
            found = files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
                .filter(n -> n.endsWith("-classes.csv"))
                .map(n -> n.substring(0, n.length() - "-classes.csv".length())).sorted().toList();
        }
        if (found.size() != 1) throw new IOException("attendu un jeu de faits dans " + dir
                + ", trouvé " + found.size() + " ; utiliser --prefix si nécessaire");
        return found.get(0);
    }
    @Override public Facts load() throws IOException {
        String stem = prefix == null ? discoverPrefix(directory) : prefix;
        if (!stem.matches("[A-Za-z0-9_.-]+") || stem.equals(".."))
            throw new IOException("préfixe invalide : " + stem);
        List<String> classes = new ArrayList<>();
        for (String[] row : Csv.read(directory.resolve(stem + "-classes.csv"), "class;simpleName;package"))
            classes.add(row[0]);
        Map<String, String> owners = new TreeMap<>();
        for (String[] row : Csv.read(directory.resolve(stem + "-methods.csv"), "method;owner")) {
            if (owners.putIfAbsent(row[0], row[1]) != null)
                throw new IOException("méthode dupliquée : " + row[0]);
        }
        List<Facts.CallArc> calls = new ArrayList<>();
        for (String[] row : Csv.read(directory.resolve(stem + "-calls.csv"),
                "caller;callerClass;callee;calleeClass;sites")) {
            if (!Objects.equals(owners.get(row[0]), row[1]) || !Objects.equals(owners.get(row[2]), row[3]))
                throw new IOException("classe propriétaire incohérente pour l'arc : " + row[0] + " -> " + row[2]);
            try { calls.add(new Facts.CallArc(row[0], row[2], Integer.parseInt(row[4]))); }
            catch (NumberFormatException e) { throw new IOException("nombre de sites invalide : " + row[4]); }
        }
        try { return new Facts(classes, owners, calls); }
        catch (IllegalArgumentException e) { throw new IOException("faits invalides : " + e.getMessage(), e); }
    }
}
