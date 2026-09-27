package hai913i.tp1.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.core.compiler.IProblem;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.callgraph.CallGraphBuilder;
import hai913i.tp1.extract.AstPrinter;
import hai913i.tp1.extract.FactExtractor;
import hai913i.tp1.metrics.MetricsService;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.report.DotReport;
import hai913i.tp1.spoon.SpoonExtractor;
import hai913i.tp1.spoon.SpoonRawCounts;
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

        // Le seuil est exige ici, et non a la lecture des arguments, pour que
        // « dossier inexistant » et « aucun fichier .java » soient diagnostiques
        // avant lui : sans cela, oublier --x masquait le vrai probleme.
        if (options.action().needsThreshold() && !options.hasThreshold()) {
            err.println("Erreur : le seuil X est obligatoire pour l'action "
                    + options.action().name().toLowerCase(Locale.ROOT) + " : ajoutez --x N");
            err.println();
            err.print(CommandLine.USAGE);
            return BAD_USAGE;
        }

        List<ParsedFile> files = JdtParser.parse(sources, List.of());
        int errors = reportCompilationProblems(files, err);

        out.println("Racine des sources     : " + sources.sourceRoot());
        out.println("Unites de compilation  : " + files.size());
        out.println("Erreurs de compilation : " + errors);
        out.println();

        // A1 : l'arbre brut, avant toute extraction de faits.
        if (options.action() == Action.AST) {
            return printAst(files, options, out, err);
        }

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
                TextReport.metrics(out, MetricsService.compute(facts, options.thresholdOrZero()));
            } catch (IOException e) {
                err.println("Erreur de lecture pendant le calcul des metriques : " + e.getMessage());
                return BAD_PROJECT;
            }
        }

        if (action == Action.SPOON) {
            ProjectFacts spoonFacts = SpoonExtractor.extract(sources.sourceRoot(),
                    sources.javaFiles());
            TextReport.comparison(out, facts, spoonFacts,
                    SpoonRawCounts.of(sources.sourceRoot()));
        }

        CallGraph graph = CallGraphBuilder.build(facts);
        if (action == Action.GRAPH || action == Action.ALL) {
            TextReport.graph(out, graph);
        }
        options.callees().ifPresent(id -> TextReport.neighbours(out, graph, id, false));
        options.callers().ifPresent(id -> TextReport.neighbours(out, graph, id, true));

        if (options.dotFile().isPresent()) {
            try (PrintStream dot = new PrintStream(
                    Files.newOutputStream(options.dotFile().get()), true, StandardCharsets.UTF_8)) {
                DotReport.write(dot, graph, options.dotPackage().orElse(null));
            } catch (IOException e) {
                err.println("Erreur d'ecriture du fichier DOT : " + e.getMessage());
                return BAD_PROJECT;
            }
            out.println();
            out.println("Graphe exporte : " + options.dotFile().get());
        }

        return OK;
    }

    private static int printAst(List<ParsedFile> files, Options options,
                                PrintStream out, PrintStream err) {
        List<ParsedFile> selected = files.stream()
                .filter(file -> options.astFile()
                        .map(name -> file.path().getFileName().toString().equals(name))
                        .orElse(true))
                .toList();
        if (selected.isEmpty()) {
            err.println("Erreur : aucun fichier nomme " + options.astFile().orElse("")
                    + " dans le projet");
            return BAD_USAGE;
        }
        for (ParsedFile file : selected) {
            out.println("== AST de " + file.path().getFileName() + " ==");
            AstPrinter printer = new AstPrinter(out);
            file.unit().accept(printer);
            out.println();
        }
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
