package hai913i.tp1.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.eclipse.jdt.core.compiler.IProblem;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.callgraph.CallGraphBuilder;
import hai913i.tp1.extract.FactExtractor;
import hai913i.tp1.metrics.MetricsService;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.report.TextReport;

/** Enchaine les etapes : sources, AST, faits, traitements, sorties. */
public final class Analyzer {

    public static final int OK = 0;
    public static final int BAD_USAGE = 2;
    public static final int BAD_PROJECT = 3;
    public static final int NO_SOURCES = 4;

    private Analyzer() {
    }

    public static int run(Options options, PrintStream out, PrintStream err) {
        ProjectSources sources;
        try {
            sources = ProjectSources.of(options.projectDirectory());
        } catch (IllegalArgumentException e) {
            err.println("Erreur : " + e.getMessage());
            return BAD_PROJECT;
        } catch (IOException e) {
            err.println("Erreur de lecture : " + e.getMessage());
            return BAD_PROJECT;
        }

        if (sources.isEmpty()) {
            err.println("Erreur : aucun fichier .java sous " + sources.sourceRoot());
            return NO_SOURCES;
        }

        List<ParsedFile> files = JdtParser.parse(sources, List.of());
        int errors = reportCompilationProblems(files, err);

        out.println("Racine des sources     : " + sources.sourceRoot());
        out.println("Unites de compilation  : " + files.size());
        out.println("Erreurs de compilation : " + errors);
        out.println();

        ProjectFacts facts = FactExtractor.extract(files);
        TextReport.summary(out, facts);

        Action action = options.action();
        if (action == Action.STRUCTURE || action == Action.ALL) {
            TextReport.structure(out, facts);
        }
        if (action == Action.CALLS || action == Action.ALL) {
            TextReport.calls(out, facts);
        }
        if (action == Action.METRICS || action == Action.ALL) {
            try {
                TextReport.metrics(out, MetricsService.compute(facts, options.threshold()));
            } catch (IOException e) {
                err.println("Erreur de lecture pendant le calcul des metriques : " + e.getMessage());
                return BAD_PROJECT;
            }
        }

        CallGraph graph = CallGraphBuilder.build(facts);
        if (action == Action.GRAPH || action == Action.ALL) {
            TextReport.graph(out, graph);
        }
        options.callees().ifPresent(id -> TextReport.neighbours(out, graph, id, false));
        options.callers().ifPresent(id -> TextReport.neighbours(out, graph, id, true));

        return OK;
    }

    private static int reportCompilationProblems(List<ParsedFile> files, PrintStream err) {
        int errors = 0;
        for (ParsedFile file : files) {
            for (IProblem problem : file.unit().getProblems()) {
                if (problem.isError()) {
                    errors++;
                    err.println(file.path().getFileName()
                            + ":" + problem.getSourceLineNumber()
                            + " " + problem.getMessage());
                }
            }
        }
        return errors;
    }
}
