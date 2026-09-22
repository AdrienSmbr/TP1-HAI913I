package hai913i.tp1.metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/**
 * Calcule les 13 metriques a partir du seul modele de faits.
 *
 * <p>Aucune classe d'{@code org.eclipse.jdt} n'apparait ici : c'est l'exigence
 * B1. Seule la question 2 relit les fichiers, pour compter des lignes physiques.</p>
 */
public final class MetricsService {

    private MetricsService() {
    }

    public static MetricsReport compute(ProjectFacts facts, int threshold) throws IOException {
        List<TypeFact> types = facts.types();
        int classCount = types.size();
        int methodCount = facts.methodCount();
        int fieldCount = facts.fieldCount();

        List<MethodFact> methods = types.stream()
                .flatMap(type -> type.methods().stream())
                .toList();
        List<MethodFact> withBody = methods.stream()
                .filter(method -> method.bodyLoc() > 0)
                .toList();
        int bodyLines = withBody.stream().mapToInt(MethodFact::bodyLoc).sum();

        List<TypeFact> topByMethods = Ranking.topTenPercent(types,
                type -> type.methods().size(), TypeFact::qualifiedName);
        List<TypeFact> topByFields = Ranking.topTenPercent(types,
                type -> type.fields().size(), TypeFact::qualifiedName);
        List<TypeFact> topByBoth = topByMethods.stream()
                .filter(topByFields::contains)
                .toList();

        List<TypeFact> overThreshold = types.stream()
                .filter(type -> type.methods().size() > threshold)
                .toList();

        Map<String, List<MethodFact>> topMethodsPerClass = new LinkedHashMap<>();
        for (TypeFact type : types) {
            topMethodsPerClass.put(type.qualifiedName(),
                    Ranking.topTenPercent(type.methods(), MethodFact::bodyLoc, MethodFact::id));
        }

        int maxParameters = methods.stream()
                .mapToInt(MethodFact::parameterCount)
                .max()
                .orElse(0);
        List<MethodFact> withMaxParameters = methods.stream()
                .filter(method -> method.parameterCount() == maxParameters)
                .toList();

        return new MetricsReport(
                classCount,
                countApplicationLines(facts.sourceFiles()),
                methodCount,
                facts.packages().size(),
                average(methodCount, classCount),
                average(bodyLines, withBody.size()),
                average(fieldCount, classCount),
                topByMethods,
                topByFields,
                topByBoth,
                threshold,
                overThreshold,
                topMethodsPerClass,
                maxParameters,
                withMaxParameters,
                withBody.size(),
                bodyLines);
    }

    /**
     * Question 2 : lignes physiques des fichiers .java analyses, lignes vides et
     * commentaires compris, ce que compte Files.readAllLines(chemin).size().
     */
    private static int countApplicationLines(List<Path> sourceFiles) throws IOException {
        int total = 0;
        for (Path file : sourceFiles) {
            total += Files.readAllLines(file, StandardCharsets.UTF_8).size();
        }
        return total;
    }

    private static double average(int total, int count) {
        return count == 0 ? 0.0 : (double) total / count;
    }
}
