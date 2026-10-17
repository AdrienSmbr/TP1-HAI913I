package hai913i.tp2;

import java.util.*;

/** B3 : coupe descendante ; une coupe incompatible est signalée, jamais réparée. */
public final class ModuleCutter {
    public record Module(ClusterNode node, double internalMean) {}
    public record Result(List<Module> modules, int maximum) {
        public Result { modules = List.copyOf(modules); }
        public boolean accepted() { return modules.size() <= maximum; }
    }
    public Result cut(ClusterNode root, WeightedGraph graph, double cp) {
        if (!Double.isFinite(cp) || cp < 0 || cp > 1)
            throw new IllegalArgumentException("CP doit être fini et compris entre 0 et 1");
        if (!root.members().equals(graph.classes()))
            throw new IllegalArgumentException("la racine doit contenir exactement les classes du graphe");
        List<Module> modules = new ArrayList<>();
        visit(root, graph, cp, modules);
        modules.sort(Comparator.comparing(m -> m.node().members().get(0)));
        return new Result(modules, graph.classes().size() / 2);
    }
    private void visit(ClusterNode node, WeightedGraph graph, double cp, List<Module> modules) {
        double mean = graph.internalMean(node.members());
        // La tolérance des égalités de fusion ne s'applique PAS au seuil strict.
        if (node.isLeaf() || mean > cp) modules.add(new Module(node, mean));
        else { visit(node.left(), graph, cp, modules); visit(node.right(), graph, cp, modules); }
    }
}
