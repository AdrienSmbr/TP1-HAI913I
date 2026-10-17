package hai913i.tp2;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Restitution seule : aucun calcul de regroupement dans les exports. */
public final class Exports {
    private Exports() {}
    public static String number(double value) { return String.format(Locale.ROOT, "%.10f", value); }
    static String quoted(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""; }
    static String shortName(String s) { return s.substring(s.lastIndexOf('.') + 1); }
    public static void save(Path out, WeightedGraph graph, AgglomerativeClusterer.Result tree,
                            ModuleCutter.Result cut, double cp) throws IOException {
        Files.createDirectories(out);
        graph.save(out.resolve("graphe.csv"));
        StringBuilder dot = new StringBuilder("graph Couplage {\n  graph [overlap=false];\n  node [shape=box, fontname=\"DejaVu Sans\"];\n");
        for (String c : graph.classes()) dot.append("  ").append(quoted(c)).append(";\n");
        graph.edges().forEach((p, w) -> dot.append("  ").append(quoted(p.a())).append(" -- ").append(quoted(p.b()))
                .append(" [label=").append(quoted(String.format(Locale.ROOT, "%.4f", w))).append("];\n"));
        Csv.write(out.resolve("graphe.dot"), dot.append("}\n").toString());
        StringBuilder matrix = new StringBuilder("class;" + String.join(";", graph.classes()) + "\n");
        for (String a : graph.classes()) {
            matrix.append(a);
            for (String b : graph.classes()) matrix.append(';').append(Double.toString(graph.weight(a, b)));
            matrix.append('\n');
        }
        Csv.write(out.resolve("couplages.csv"), matrix.toString());
        StringBuilder merges = new StringBuilder("step;id;left;right;similarity;internalMean;members\n");
        int step = 0;
        for (ClusterNode n : tree.merges()) merges.append(++step).append(';').append(n.id()).append(';')
                .append(n.left().id()).append(';').append(n.right().id()).append(';')
                .append(Double.toString(n.similarity())).append(';').append(Double.toString(graph.internalMean(n.members())))
                .append(';').append(String.join("|", n.members())).append('\n');
        Csv.write(out.resolve("fusions.csv"), merges.toString());
        StringBuilder text = new StringBuilder(); treeText(tree.root(), "", text);
        Csv.write(out.resolve("dendrogramme.txt"), text.toString());
        StringBuilder treeDot = new StringBuilder("digraph Dendrogramme {\n  node [fontname=\"DejaVu Sans\", shape=box];\n");
        treeDot(tree.root(), treeDot); Csv.write(out.resolve("dendrogramme.dot"), treeDot.append("}\n").toString());
        StringBuilder modules = new StringBuilder("module;node;size;internalMean;cp;accepted;classes\n");
        int i = 0;
        for (ModuleCutter.Module m : cut.modules()) modules.append(++i).append(';').append(m.node().id()).append(';')
                .append(m.node().members().size()).append(';').append(m.node().isLeaf() ? "NA" : Double.toString(m.internalMean()))
                .append(';').append(cp).append(';').append(cut.accepted()).append(';')
                .append(String.join("|", m.node().members())).append('\n');
        Csv.write(out.resolve("modules.csv"), modules.toString());
        StringBuilder moduleDot = new StringBuilder("graph Modules {\n  node [shape=box, fontname=\"DejaVu Sans\"];\n");
        Map<String, Integer> assignment = new HashMap<>();
        i = 0;
        for (ModuleCutter.Module m : cut.modules()) {
            int id = ++i;
            moduleDot.append("  subgraph cluster_").append(id).append(" { label=")
                .append(quoted("Module " + id + " | C=" + (m.node().isLeaf() ? "NA" : number(m.internalMean())))).append(';');
            for (String c : m.node().members()) { assignment.put(c, id); moduleDot.append(quoted(c)).append(';'); }
            moduleDot.append("}\n");
        }
        graph.edges().forEach((p, w) -> moduleDot.append(quoted(p.a())).append(" -- ").append(quoted(p.b()))
            .append(" [style=").append(assignment.get(p.a()).equals(assignment.get(p.b())) ? "solid" : "dashed")
            .append(", label=").append(quoted(String.format(Locale.ROOT, "%.4f", w))).append("];\n"));
        Csv.write(out.resolve("modules.dot"), moduleDot.append("}\n").toString());
    }
    /** Dossier complet cohérent, y compris après un changement interactif de CP. */
    public static void saveBundle(Path out, WeightedGraph graph, AgglomerativeClusterer.Result tree,
            ModuleCutter.Result cut, double cp, AgglomerativeClusterer.Linkage linkage,
            String relation, String header) throws IOException {
        save(out, graph, tree, cut, cp);
        Csv.write(out.resolve("resultats.txt"), AnalysisReport.format(graph, tree, cut, cp, linkage, header));
        Csv.write(out.resolve("parametres.txt"), "linkage=" + linkage.name().toLowerCase(Locale.ROOT)
                + "\nrelation=" + relation + "\ncp=" + cp + "\n");
    }
    private static void treeText(ClusterNode n, String indent, StringBuilder b) {
        b.append(indent).append('#').append(n.id()).append(' ');
        if (n.isLeaf()) b.append(n.members().get(0));
        else b.append("S=").append(number(n.similarity())).append(" n=").append(n.members().size());
        b.append('\n');
        if (!n.isLeaf()) { treeText(n.left(), indent + "  ", b); treeText(n.right(), indent + "  ", b); }
    }
    private static void treeDot(ClusterNode n, StringBuilder b) {
        b.append("  n").append(n.id()).append(" [label=")
            .append(quoted(n.isLeaf() ? n.members().get(0) : "#" + n.id() + " S=" + number(n.similarity()))).append("];\n");
        if (!n.isLeaf()) {
            b.append("  n").append(n.id()).append(" -> n").append(n.left().id()).append(";\n")
                .append("  n").append(n.id()).append(" -> n").append(n.right().id()).append(";\n");
            treeDot(n.left(), b); treeDot(n.right(), b);
        }
    }
}
