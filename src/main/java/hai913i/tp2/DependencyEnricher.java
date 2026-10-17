package hai913i.tp2;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** C2 : une unité par arc d'appel et par relation structurelle dirigée distincte. */
public final class DependencyEnricher {
    public record Relation(String type, String target, String kind) {}
    public record Result(WeightedGraph graph, int callArcs, int structuralRelations,
                         int externalRelations) {
        public int denominator() { return callArcs + structuralRelations; }
    }
    public List<Relation> load(Path path, List<String> classes) throws IOException {
        Set<String> known = new HashSet<>(classes);
        Set<Relation> unique = new HashSet<>();
        List<Relation> result = new ArrayList<>();
        for (String[] row : Csv.read(path, "type;target;kind")) {
            Relation r = new Relation(row[0], row[1], row[2]);
            if (!known.contains(r.type()) || r.target().isBlank() || r.type().equals(r.target()))
                throw new IOException("relation structurelle invalide : " + r);
            if (!Set.of("extends", "implements").contains(r.kind()))
                throw new IOException("relation attendue : extends ou implements");
            if (!unique.add(r)) throw new IOException("relation structurelle dupliquée : " + r);
            result.add(r);
        }
        return List.copyOf(result);
    }
    public Result enrich(Facts facts, List<Relation> relations) {
        CouplingCalculator.Result base = new CouplingCalculator().compute(facts);
        Map<WeightedGraph.Pair, Integer> counts = new TreeMap<>(base.arcCounts());
        Set<String> classes = new HashSet<>(facts.classes());
        Set<Relation> unique = new HashSet<>();
        int internal = 0, external = 0;
        for (Relation r : relations) {
            if (!classes.contains(r.type()) || r.type().equals(r.target()) || r.target().isBlank()
                    || !Set.of("extends", "implements").contains(r.kind()) || !unique.add(r))
                throw new IllegalArgumentException("relation invalide ou dupliquée : " + r);
            if (!classes.contains(r.target())) { external++; continue; }
            counts.merge(new WeightedGraph.Pair(r.type(), r.target()), 1, Integer::sum);
            internal++;
        }
        int denominator = base.inter() + internal;
        Map<WeightedGraph.Pair, Double> weights = new TreeMap<>();
        counts.forEach((pair, count) -> weights.put(pair, count / (double) denominator));
        return new Result(new WeightedGraph(facts.classes(), weights), base.inter(), internal, external);
    }
}
