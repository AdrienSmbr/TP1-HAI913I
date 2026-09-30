package hai913i.tp1.callgraph;

import java.util.List;
import java.util.Map;

public record CallGraph(List<String> nodes,
                        Map<String, Map<String, Integer>> outgoing,
                        Map<String, Map<String, Integer>> incoming,
                        int externalCalls,
                        int unresolvedCalls) {

    public int nodeCount() {
        return nodes.size();
    }

    public int arcCount() {
        return outgoing.values().stream().mapToInt(Map::size).sum();
    }

    public int siteCount() {
        return outgoing.values().stream()
                .flatMap(targets -> targets.values().stream())
                .mapToInt(Integer::intValue)
                .sum();
    }

    public Map<String, Integer> callees(String methodId) {
        return outgoing.getOrDefault(methodId, Map.of());
    }

    public Map<String, Integer> callers(String methodId) {
        return incoming.getOrDefault(methodId, Map.of());
    }

    public boolean contains(String methodId) {
        return nodes.contains(methodId);
    }
}
