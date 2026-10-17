package hai913i.tp2;

import java.util.*;

/** A1 : une unité par arc inter-classes, indépendamment du nombre de sites. */
public final class CouplingCalculator {
    public record Result(WeightedGraph graph, int intra, int inter,
                         Map<WeightedGraph.Pair, Integer> arcCounts) {}
    public Result compute(Facts facts) {
        Map<WeightedGraph.Pair, Integer> counts = new TreeMap<>();
        int intra = 0, inter = 0;
        for (Facts.CallArc arc : facts.calls()) {
            String a = facts.methodOwners().get(arc.caller());
            String b = facts.methodOwners().get(arc.callee());
            if (a.equals(b)) { intra++; continue; }
            inter++;
            counts.merge(new WeightedGraph.Pair(a, b), 1, Integer::sum);
        }
        Map<WeightedGraph.Pair, Double> weights = new TreeMap<>();
        final int denominator = inter;
        // Si Einter est vide, la relation est vide : tous les poids valent zéro.
        counts.forEach((pair, count) -> weights.put(pair, count / (double) denominator));
        return new Result(new WeightedGraph(facts.classes(), weights), intra, inter,
                Collections.unmodifiableMap(counts));
    }
}
