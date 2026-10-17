package hai913i.tp2;

import java.util.*;

/** B1/B2 et C1 : lien moyen imposé ou lien simple explicitement demandé. */
public final class AgglomerativeClusterer {
    public enum Linkage {
        AVERAGE, SINGLE;
        public static Linkage parse(String name) {
            return switch (name) {
                case "average" -> AVERAGE;
                case "single" -> SINGLE;
                default -> throw new IllegalArgumentException("liaison attendue : average ou single");
            };
        }
    }
    private final Linkage linkage;
    public AgglomerativeClusterer() { this(Linkage.AVERAGE); }
    public AgglomerativeClusterer(Linkage linkage) { this.linkage = Objects.requireNonNull(linkage); }
    public static final double EPSILON = 1e-9;
    public record Result(ClusterNode root, List<ClusterNode> merges) {
        public Result { merges = List.copyOf(merges); }
    }
    private record Candidate(int a, int b, double similarity) {}
    public double similarity(WeightedGraph graph, ClusterNode a, ClusterNode b) {
        double sum = 0, maximum = 0;
        for (String x : a.members()) for (String y : b.members()) {
            double weight = graph.weight(x, y);
            sum += weight; maximum = Math.max(maximum, weight);
        }
        if (linkage == Linkage.SINGLE) return maximum;
        return sum / ((double) a.members().size() * b.members().size());
    }
    public Result cluster(WeightedGraph graph) {
        TreeMap<Integer, ClusterNode> active = new TreeMap<>();
        for (int i = 0; i < graph.classes().size(); i++)
            active.put(i, ClusterNode.leaf(i, graph.classes().get(i)));
        List<ClusterNode> merges = new ArrayList<>();
        int nextId = active.size();
        while (active.size() > 1) {
            List<ClusterNode> nodes = new ArrayList<>(active.values());
            List<Candidate> candidates = new ArrayList<>();
            double maximum = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < nodes.size(); i++) for (int j = i + 1; j < nodes.size(); j++) {
                double s = similarity(graph, nodes.get(i), nodes.get(j));
                candidates.add(new Candidate(nodes.get(i).id(), nodes.get(j).id(), s));
                maximum = Math.max(maximum, s);
            }
            // Deux passes : tolérance relative au vrai maximum, sans dérive d'égalité.
            Candidate best = null;
            for (Candidate c : candidates) {
                if (maximum - c.similarity() <= EPSILON) { best = c; break; }
            }
            if (best == null) throw new IllegalStateException("aucune fusion possible");
            ClusterNode a = active.remove(best.a()), b = active.remove(best.b());
            ClusterNode merged = ClusterNode.merge(nextId++, a, b, best.similarity());
            active.put(merged.id(), merged); merges.add(merged);
        }
        return new Result(active.firstEntry().getValue(), merges);
    }
}
