package hai913i.tp2;

import java.util.*;
import java.io.IOException;
import java.nio.file.Path;

/** Modèle partagé par les interactions Swing et leurs tests, sans calcul dupliqué. */
public final class ExplorerModel {
    private final WeightedGraph graph;
    private AgglomerativeClusterer.Linkage linkage;
    private AgglomerativeClusterer.Result tree;
    private ModuleCutter.Result cut;
    private double cp;
    private final String relation;
    private final String header;
    public ExplorerModel(WeightedGraph graph, double cp, AgglomerativeClusterer.Linkage linkage) {
        this(graph, cp, linkage, "reloaded-graph", "C3 | Graphe chargé dans l'explorateur (compteurs de faits indisponibles)\n");
    }
    public ExplorerModel(WeightedGraph graph, double cp, AgglomerativeClusterer.Linkage linkage,
            String relation, String header) {
        this.relation = Objects.requireNonNull(relation);
        this.header = Objects.requireNonNull(header);
        this.graph = Objects.requireNonNull(graph);
        setLinkage(linkage); setCp(cp);
    }
    public void export(Path path) throws IOException {
        Exports.saveBundle(path, graph, tree, cut, cp, linkage, relation, header);
    }
    public WeightedGraph graph() { return graph; }
    public AgglomerativeClusterer.Result tree() { return tree; }
    public ModuleCutter.Result cut() { return cut; }
    public double cp() { return cp; }
    public AgglomerativeClusterer.Linkage linkage() { return linkage; }
    public void setCp(double value) {
        ModuleCutter.Result next = new ModuleCutter().cut(tree.root(), graph, value);
        cp = value; cut = next;
    }
    public void setLinkage(AgglomerativeClusterer.Linkage value) {
        linkage = Objects.requireNonNull(value);
        tree = new AgglomerativeClusterer(linkage).cluster(graph);
        cut = new ModuleCutter().cut(tree.root(), graph, cp);
    }
    public List<String> search(String query) {
        String q = query.strip().toLowerCase(Locale.ROOT);
        return graph.classes().stream().filter(c -> c.toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    public Map<String, Integer> assignment() {
        Map<String, Integer> result = new TreeMap<>(); int i = 0;
        for (var m : cut.modules()) { int id = ++i; m.node().members().forEach(c -> result.put(c, id)); }
        return Collections.unmodifiableMap(result);
    }
}
