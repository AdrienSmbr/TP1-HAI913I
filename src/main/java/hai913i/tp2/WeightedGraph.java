package hai913i.tp2;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Graphe immuable creux. Une paire absente vaut zéro ; les isolés restent des nœuds. */
public final class WeightedGraph {
    public record Pair(String a, String b) implements Comparable<Pair> {
        public Pair {
            Objects.requireNonNull(a); Objects.requireNonNull(b);
            if (a.compareTo(b) > 0) { String tmp = a; a = b; b = tmp; }
            if (a.equals(b)) throw new IllegalArgumentException("boucle interdite : " + a);
        }
        @Override public int compareTo(Pair other) {
            int c = a.compareTo(other.a); return c != 0 ? c : b.compareTo(other.b);
        }
    }
    private final List<String> classes;
    private final Set<String> classSet;
    private final NavigableMap<Pair, Double> edges;
    public WeightedGraph(List<String> names, Map<Pair, Double> weights) {
        classes = names.stream().sorted().toList();
        Set<String> unique = new HashSet<>(classes);
        if (classes.isEmpty() || unique.size() != classes.size())
            throw new IllegalArgumentException("nœuds vides ou dupliqués");
        classes.forEach(Csv::field);
        classSet = Set.copyOf(classes);
        TreeMap<Pair, Double> copy = new TreeMap<>();
        weights.forEach((p, w) -> {
            if (!unique.contains(p.a()) || !unique.contains(p.b()))
                throw new IllegalArgumentException("extrémité absente : " + p);
            if (!Double.isFinite(w) || w <= 0 || w > 1)
                throw new IllegalArgumentException("poids hors ]0,1] : " + w);
            copy.put(p, w);
        });
        edges = Collections.unmodifiableNavigableMap(copy);
    }
    public List<String> classes() { return classes; }
    public NavigableMap<Pair, Double> edges() { return edges; }
    public double weight(String a, String b) {
        if (!classSet.contains(a) || !classSet.contains(b))
            throw new IllegalArgumentException("classe inconnue : " + a + " / " + b);
        return a.equals(b) ? 0 : edges.getOrDefault(new Pair(a, b), 0.0);
    }
    public List<String> isolated() {
        Set<String> linked = new HashSet<>();
        edges.keySet().forEach(p -> { linked.add(p.a()); linked.add(p.b()); });
        return classes.stream().filter(c -> !linked.contains(c)).toList();
    }
    public List<Map.Entry<Pair, Double>> rankedEdges() {
        return edges.entrySet().stream().sorted(Map.Entry.<Pair, Double>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey())).toList();
    }
    public double internalMean(List<String> members) {
        if (members.isEmpty() || new HashSet<>(members).size() != members.size() || !classSet.containsAll(members))
            throw new IllegalArgumentException("groupe vide, dupliqué ou contenant une classe inconnue");
        if (members.size() < 2) return 0; // Non applicable : feuille.
        double sum = 0;
        for (int i = 0; i < members.size(); i++)
            for (int j = i + 1; j < members.size(); j++) sum += weight(members.get(i), members.get(j));
        return sum / ((double) members.size() * (members.size() - 1) / 2);
    }
    public void save(Path path) throws IOException {
        StringBuilder b = new StringBuilder("kind;classA;classB;weight\n");
        for (String c : classes) b.append("node;").append(c).append(";;\n");
        edges.forEach((p, w) -> b.append("edge;").append(p.a()).append(';').append(p.b())
                .append(';').append(Double.toString(w)).append('\n'));
        Csv.write(path, b.toString());
    }
    public static WeightedGraph load(Path path) throws IOException {
        List<String> classes = new ArrayList<>();
        Map<Pair, Double> edges = new TreeMap<>();
        try {
            for (String[] r : Csv.read(path, "kind;classA;classB;weight")) {
                if (r[0].equals("node") && r[2].isEmpty() && r[3].isEmpty()) classes.add(r[1]);
                else if (r[0].equals("edge")) {
                    Pair p = new Pair(r[1], r[2]);
                    if (edges.putIfAbsent(p, Double.parseDouble(r[3])) != null)
                        throw new IllegalArgumentException("arête dupliquée : " + p);
                } else throw new IllegalArgumentException("ligne de graphe inconnue : " + r[0]);
            }
            double sum = edges.values().stream().mapToDouble(Double::doubleValue).sum();
            if (!edges.isEmpty() && Math.abs(sum - 1) > 1e-9)
                throw new IllegalArgumentException("graphe de couplage non normalisé : somme = " + sum + " (1 attendu)");
            return new WeightedGraph(classes, edges);
        } catch (IllegalArgumentException e) { throw new IOException("graphe invalide : " + e.getMessage(), e); }
    }
}
