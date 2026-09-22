package hai913i.tp1.callgraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/**
 * Construit le graphe d'appel a partir du seul modele de faits.
 * Aucune classe d'org.eclipse.jdt n'intervient ici (exigence B1).
 */
public final class CallGraphBuilder {

    private CallGraphBuilder() {
    }

    public static CallGraph build(ProjectFacts facts) {
        List<String> nodes = facts.types().stream()
                .flatMap(type -> type.methods().stream())
                .map(MethodFact::id)
                .sorted()
                .toList();

        Map<String, Map<String, Integer>> outgoing = new TreeMap<>();
        Map<String, Map<String, Integer>> incoming = new TreeMap<>();
        int external = 0;
        int unresolved = 0;

        for (TypeFact type : facts.types()) {
            for (MethodFact method : type.methods()) {
                for (CallFact call : method.calls()) {
                    if (!call.resolved()) {
                        unresolved++;
                        continue;
                    }
                    String target = call.targetId().orElseThrow();
                    if (!call.internal() || !nodes.contains(target)) {
                        external++;
                        continue;
                    }
                    add(outgoing, method.id(), target);
                    add(incoming, target, method.id());
                }
            }
        }
        return new CallGraph(nodes, freeze(outgoing), freeze(incoming), external, unresolved);
    }

    private static void add(Map<String, Map<String, Integer>> index, String from, String to) {
        index.computeIfAbsent(from, key -> new TreeMap<>()).merge(to, 1, Integer::sum);
    }

    /** Copies triees : deux executions doivent donner la meme sortie. */
    private static Map<String, Map<String, Integer>> freeze(Map<String, Map<String, Integer>> index) {
        Map<String, Map<String, Integer>> frozen = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(index.keySet());
        keys.sort(Comparator.naturalOrder());
        for (String key : keys) {
            frozen.put(key, Map.copyOf(index.get(key)));
        }
        return frozen;
    }
}
