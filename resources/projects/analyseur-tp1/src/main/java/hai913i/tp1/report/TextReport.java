package hai913i.tp1.report;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.spoon.SpoonRawCounts;

public final class TextReport {

    private TextReport() {
    }

    public static void summary(PrintStream out, ProjectFacts facts) {
        out.println("Types      : " + facts.types().size());
        out.println("Methodes   : " + facts.methodCount()
                + " (dont " + facts.constructorCount() + " constructeurs)");
        out.println("Attributs  : " + facts.fieldCount());
        out.println("Paquetages : " + facts.packages().size() + " " + facts.packages());
        out.println("Appels     : " + facts.callCount()
                + " (" + facts.internalCallCount() + " internes, "
                + facts.externalCallCount() + " externes, "
                + facts.unresolvedCallCount() + " non resolus)");
    }

    public static void structure(PrintStream out, ProjectFacts facts) {
        out.println();
        out.println("== Structure ==");
        for (TypeFact type : facts.types()) {
            out.println(type.kind() + " " + type.qualifiedName() + "  [" + type.packageName() + "]");
            if (!type.superclasses().isEmpty()) {
                out.println("    superclasses : " + type.superclasses());
            }
            if (!type.interfaces().isEmpty()) {
                out.println("    interfaces   : " + type.interfaces());
            }
            for (FieldFact field : type.fields()) {
                out.println("    attribut " + field.visibility()
                        + " " + field.declaredType() + " " + field.name());
            }
            for (MethodFact method : type.methods()) {
                out.println("    methode  " + method.visibility()
                        + " " + method.name() + "/" + method.parameterCount()
                        + (method.constructor() ? " (constructeur)" : "")
                        + " corps=" + method.bodyLoc() + "l");
            }
        }
    }

    public static void calls(PrintStream out, ProjectFacts facts) {
        out.println();
        out.println("== Appels et receveurs ==");
        for (TypeFact type : facts.types()) {
            for (MethodFact method : type.methods()) {
                if (method.calls().isEmpty()) {
                    continue;
                }
                out.println(method.id());
                for (CallFact call : method.calls()) {
                    out.println("    ligne " + call.sourceLine()
                            + " : " + call.calledName()
                            + " | receveur " + call.staticReceiverType()
                            + " | cible " + call.targetId().orElse("NON RESOLUE")
                            + (call.resolved() ? (call.internal() ? " [interne]" : " [externe]") : ""));
                }
            }
        }
    }

    public static void metrics(PrintStream out, MetricsReport report) {
        out.println();
        out.println("== Metriques ==");
        out.println(" 1. Classes                      : " + report.classCount());
        out.println(" 2. Lignes de l'application      : " + report.applicationLines());
        out.println(" 3. Methodes                     : " + report.methodCount());
        out.println(" 4. Paquetages                   : " + report.packageCount());
        out.println(" 5. Methodes par classe          : " + twoDecimals(report.methodsPerClass()));
        out.println(" 6. Lignes par methode (a corps) : " + twoDecimals(report.linesPerMethod())
                + "  (" + report.bodyLinesTotal() + " lignes pour "
                + report.methodsWithBody() + " methodes)");
        out.println(" 7. Attributs par classe         : " + twoDecimals(report.fieldsPerClass()));
        out.println(" 8. 10 % ayant le plus de methodes  : " + names(report.topByMethods()));
        out.println(" 9. 10 % ayant le plus d'attributs  : " + names(report.topByFields()));
        out.println("10. Intersection des deux          : " + names(report.topByBoth()));
        out.println("11. Plus de " + report.threshold() + " methodes        : "
                + names(report.overThreshold()));
        out.println("12. Par classe, 10 % des methodes les plus longues :");
        for (Map.Entry<String, List<MethodFact>> entry : report.topMethodsPerClass().entrySet()) {
            out.println("      " + entry.getKey() + " : " + methodNames(entry.getValue()));
        }
        out.println("13. Maximum de parametres       : " + report.maxParameters()
                + " " + methodIds(report.methodsWithMaxParameters()));
    }

    public static void graph(PrintStream out, CallGraph graph) {
        out.println();
        out.println("== Graphe d'appel ==");
        out.println("Noeuds : " + graph.nodeCount()
                + " | Arcs : " + graph.arcCount()
                + " | Sites d'appel internes : " + graph.siteCount());
        out.println("Appels externes : " + graph.externalCalls()
                + " | Appels non resolus : " + graph.unresolvedCalls());
        out.println();
        for (Map.Entry<String, Map<String, Integer>> source : graph.outgoing().entrySet()) {
            for (Map.Entry<String, Integer> target : source.getValue().entrySet()) {
                out.println(source.getKey() + " -> " + target.getKey()
                        + "  (poids " + target.getValue() + ")");
            }
        }
    }

    public static void neighbours(PrintStream out, CallGraph graph, String methodId, boolean callers) {
        out.println();
        String direction = callers ? "Appelantes de " : "Appelees par ";
        out.println("== " + direction + methodId + " ==");
        if (!graph.contains(methodId)) {
            out.println("Methode inconnue dans le graphe.");
            return;
        }
        Map<String, Integer> neighbours = callers ? graph.callers(methodId) : graph.callees(methodId);
        if (neighbours.isEmpty()) {
            out.println("(aucune)");
            return;
        }
        neighbours.forEach((id, weight) -> out.println("    " + id + "  (poids " + weight + ")"));
    }

    public static void comparison(PrintStream out, ProjectFacts jdt, ProjectFacts spoon,
            SpoonRawCounts raw) {
        out.println();
        out.println("== JDT contre Spoon (bonus C1) ==");
        out.printf("%-26s %12s %12s %12s%n", "MESURE", "JDT", "SPOON BRUT", "SPOON (TP)");
        row(out, "types", jdt.types().size(),
                raw.allTypes() + " / " + raw.topLevelTypes(), spoon.types().size());
        row(out, "methodes", jdt.methodCount(),
                raw.methods() + " + " + raw.constructors(), spoon.methodCount());
        row(out, "dont constructeurs", jdt.constructorCount(),
                raw.constructors() + " (" + raw.implicitConstructors() + " impl.)",
                spoon.constructorCount());
        row(out, "attributs", jdt.fieldCount(),
                raw.fields() + " (" + raw.enumValues() + " const.)", spoon.fieldCount());
        row(out, "paquetages", jdt.packages().size(),
                String.valueOf(raw.packages()), spoon.packages().size());
        row(out, "appels", jdt.callCount(),
                raw.invocations() + " (" + raw.constructorInvocations() + " ctors)",
                spoon.callCount());
        out.println();
        out.println("Les colonnes JDT et SPOON (TP) doivent coincider : c'est l'interet du");
        out.println("modele de faits, les traitements ne changent pas d'un extracteur a l'autre.");
    }

    private static void row(PrintStream out, String label, int jdt, String raw, int spoon) {
        out.printf("%-26s %12d %12s %12d%s%n", label, jdt, raw, spoon,
                jdt == spoon ? "" : "   <-- ECART");
    }

    private static String twoDecimals(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String names(List<TypeFact> types) {
        return types.isEmpty() ? "(aucune)"
                : types.stream().map(TypeFact::qualifiedName).toList().toString();
    }

    private static String methodNames(List<MethodFact> methods) {
        return methods.isEmpty() ? "(aucune)"
                : methods.stream()
                        .map(method -> method.name() + " (" + method.bodyLoc() + "l)")
                        .toList().toString();
    }

    private static String methodIds(List<MethodFact> methods) {
        return methods.stream().map(MethodFact::id).toList().toString();
    }
}
