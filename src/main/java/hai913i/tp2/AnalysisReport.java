package hai913i.tp2;

/** Résumé unique pour la CLI et les exports de l'explorateur. */
public final class AnalysisReport {
    private AnalysisReport() {}
    public static String format(WeightedGraph graph, AgglomerativeClusterer.Result tree,
            ModuleCutter.Result cut, double cp, AgglomerativeClusterer.Linkage linkage, String header) {
        StringBuilder report = new StringBuilder(header);
            report.append("A1 | Somme sur les couples non ordonnés : ")
                .append(Exports.number(graph.edges().values().stream().mapToDouble(Double::doubleValue).sum())).append('\n');
            report.append("A2 | Nœuds : ").append(graph.classes().size()).append(" | Arêtes : ").append(graph.edges().size()).append('\n');
            report.append("A2 | Classes isolées : ").append(String.join(", ", graph.isolated())).append('\n');
            report.append("A1/A2 | Couples par couplage décroissant (les cinq premiers en tête) :\n");
            for (var edge : graph.rankedEdges()) report.append("  ").append(edge.getKey().a()).append(" -- ")
                .append(edge.getKey().b()).append(" : ").append(Exports.number(edge.getValue())).append('\n');
            if (linkage == AgglomerativeClusterer.Linkage.SINGLE)
                report.append("C1 | Variante : lien simple (maximum des couplages intergroupes)\n");
            report.append("B1/B2 | Fusions : ").append(tree.merges().size()).append('\n');
            int step = 0;
            for (var n : tree.merges()) report.append("  ").append(++step).append(" : (").append(n.left().id())
                .append(',').append(n.right().id()).append(") -> ").append(n.id()).append(" | S=")
                .append(Exports.number(n.similarity())).append('\n');
            report.append("B3 | CP=").append(cp).append(" | Modules : ").append(cut.modules().size())
                .append(" | Maximum M/2 : ").append(cut.maximum()).append('\n');
            int module = 0;
            for (var m : cut.modules()) report.append("  Module ").append(++module).append(" | n=")
                .append(m.node().members().size()).append(" | C=")
                .append(m.node().isLeaf() ? "NA (feuille)" : Exports.number(m.internalMean()))
                .append(" | ").append(String.join(", ", m.node().members())).append('\n');
            report.append(cut.accepted() ? "B3 | COUPE ACCEPTÉE\n" : "B3 | COUPE REJETÉE : contrainte M/2 violée\n");
        return report.toString();
    }
}
