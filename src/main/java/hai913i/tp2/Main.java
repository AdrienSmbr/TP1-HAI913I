package hai913i.tp2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Assemblage de la chaîne, arguments, diagnostics et codes de sortie. */
public final class Main {
    private Main() {}
    private static final String USAGE = """
        Usage : java -jar target/hai913i-tp1-analyzer.jar tp2 DOSSIER --cp N [--prefix NOM] [--out DOSSIER]
             ou java -jar target/hai913i-tp1-analyzer.jar tp2 --graph FICHIER.csv --cp N [--out DOSSIER]
        ou : java -jar target/hai913i-tp1-analyzer.jar tp2 --sources PROJET_JAVA --cp N [--gui]
        Options bonus : --linkage average|single ; --inheritance FICHIER.csv ; --gui
        CP obligatoire, fini et compris entre 0 et 1 (point décimal).
        Codes : 0 = coupe acceptée, 2 = entrée invalide, 3 = coupe rejetée (exports conservés).
        --graph recharge notre export pondéré ; les compteurs de faits ne sont alors pas disponibles.
        """;
    public static void main(String[] args) {
        int code = run(args, System.out, System.err, true);
        // La boucle d'événements Swing doit rester vivante une fois la fenêtre ouverte.
        if (code != 2 && Arrays.asList(args).contains("--gui")) return;
        System.exit(code);
    }
    public static int run(String[] args, PrintStream out, PrintStream err) {
        return run(args, out, err, false);
    }
    private static int run(String[] args, PrintStream out, PrintStream err, boolean exitOnClose) {
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
            out.print(USAGE); return 0;
        }
        try {
            Path input = null, graphFile = null, sourceProject = null, inheritance = null, output = Path.of("sorties");
            boolean gui = false;
            AgglomerativeClusterer.Linkage linkage = AgglomerativeClusterer.Linkage.AVERAGE;
            String prefix = null; Double cp = null;
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--gui")) {
                    if (gui) throw new IllegalArgumentException("option répétée : --gui");
                    gui = true; continue;
                }
                if (arg.startsWith("--")) {
                    if (!Set.of("--cp", "--prefix", "--out", "--graph", "--linkage", "--inheritance", "--sources").contains(arg))
                        throw new IllegalArgumentException("option inconnue : " + arg);
                    if (!seen.add(arg)) throw new IllegalArgumentException("option répétée : " + arg);
                    if (++i == args.length) throw new IllegalArgumentException("valeur absente pour " + arg);
                    switch (arg) {
                        case "--sources" -> sourceProject = Path.of(args[i]);
                        case "--cp" -> cp = Double.parseDouble(args[i]);
                        case "--prefix" -> prefix = args[i];
                        case "--out" -> output = Path.of(args[i]);
                        case "--graph" -> graphFile = Path.of(args[i]);
                        case "--inheritance" -> inheritance = Path.of(args[i]);
                        case "--linkage" -> linkage = AgglomerativeClusterer.Linkage.parse(args[i]);
                        default -> throw new IllegalStateException();
                    }
                } else if (input == null) input = Path.of(arg);
                else throw new IllegalArgumentException("argument inattendu : " + arg);
            }
            if (cp == null || !Double.isFinite(cp) || cp < 0 || cp > 1)
                throw new IllegalArgumentException("--cp obligatoire : nombre fini entre 0 et 1");
            if ((input != null ? 1 : 0) + (graphFile != null ? 1 : 0) + (sourceProject != null ? 1 : 0) != 1)
                throw new IllegalArgumentException("indiquer un dossier de faits, --graph OU --sources");
            if ((graphFile != null || sourceProject != null) && prefix != null) throw new IllegalArgumentException("--prefix exige un dossier de faits");
            if (graphFile != null && inheritance != null)
                throw new IllegalArgumentException("--inheritance exige les faits d'appel, pas --graph");
            if (gui && java.awt.GraphicsEnvironment.isHeadless())
                throw new IllegalArgumentException("--gui exige un écran graphique (DISPLAY sous Linux)");
            StringBuilder report = new StringBuilder();
            WeightedGraph graph;
            if (graphFile != null) {
                graph = WeightedGraph.load(graphFile);
                report.append("A2 | Graphe rechargé : ").append(graphFile).append('\n');
            } else {
                FactsSource source = sourceProject == null ? new CsvFactsSource(input, prefix)
                        : new hai913i.integration.Tp1FactsSource(sourceProject);
                Facts facts = source.load();
                if (sourceProject != null) report.append("Entrée : extraction JDT directe du TP1, cibles implicites résolues conservées\n");
                CouplingCalculator.Result coupling = new CouplingCalculator().compute(facts);
                graph = coupling.graph();
                if (inheritance != null) {
                    DependencyEnricher enricher = new DependencyEnricher();
                    var enriched = enricher.enrich(facts, enricher.load(inheritance, facts.classes()));
                    graph = enriched.graph();
                    report.append("C2 | Appels + héritage/implémentation : ")
                        .append(enriched.callArcs()).append(" arcs + ").append(enriched.structuralRelations())
                        .append(" relations internes ; dénominateur = ").append(enriched.denominator())
                        .append(" ; relations externes ignorées = ").append(enriched.externalRelations()).append('\n');
                }
                report.append("A0 | Classes : ").append(facts.classes().size()).append(" | Méthodes : ")
                    .append(facts.methodOwners().size()).append(" | Arcs : ").append(facts.calls().size()).append('\n');
                report.append("A1 | Arcs internes écartés : ").append(coupling.intra())
                    .append(" | Arcs inter-classes : ").append(coupling.inter()).append('\n');
                if (graph.edges().isEmpty()) report.append("A1 | Aucune relation inter-classes : couplages tous nuls, somme = 0.\n");
            }
            var tree = new AgglomerativeClusterer(linkage).cluster(graph);
            var cut = new ModuleCutter().cut(tree.root(), graph, cp);
            String header = report.toString();
            report = new StringBuilder(AnalysisReport.format(graph, tree, cut, cp, linkage, header));
            String relation = inheritance == null ? (graphFile == null ? "calls" : "reloaded-graph") : "calls+inheritance";
            Exports.saveBundle(output, graph, tree, cut, cp, linkage, relation, header);
            out.print(report);
            if (gui) ExplorerWindow.open(new ExplorerModel(graph, cp, linkage, relation, header),
                    exitOnClose ? System::exit : null);
            return cut.accepted() ? 0 : 3;
        } catch (IOException | IllegalArgumentException e) {
            err.println("Erreur : " + e.getMessage()); err.print(USAGE); return 2;
        }
    }
}
