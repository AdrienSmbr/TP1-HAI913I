package hai913i.tp2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Format canonique : UTF-8, point-virgule, sans échappement de champs. */
final class Csv {
    private Csv() {}
    static List<String[]> read(Path path, String header) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("fichier introuvable : " + path);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).replace("\uFEFF", "").strip().equals(header))
            throw new IOException(path + " : en-tête attendu : " + header);
        int columns = header.split(";", -1).length;
        List<String[]> result = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            String[] row = lines.get(i).split(";", -1);
            if (row.length != columns) throw new IOException(path + ": ligne " + (i + 1)
                    + " : " + columns + " colonnes attendues");
            for (int j = 0; j < row.length; j++) row[j] = row[j].strip();
            result.add(row);
        }
        return result;
    }
    static void write(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
    static void field(String value) {
        if (value.isBlank() || value.contains(";") || value.contains("\"") || value.contains("|") || value.contains("\n") || value.contains("\r"))
            throw new IllegalArgumentException("champ CSV non représentable : " + value);
    }
}
