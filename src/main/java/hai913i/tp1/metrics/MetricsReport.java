package hai913i.tp1.metrics;

import java.util.List;
import java.util.Map;

import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.TypeFact;

public record MetricsReport(int classCount,
                            int applicationLines,
                            int methodCount,
                            int packageCount,
                            double methodsPerClass,
                            double linesPerMethod,
                            double fieldsPerClass,
                            List<TypeFact> topByMethods,
                            List<TypeFact> topByFields,
                            List<TypeFact> topByBoth,
                            int threshold,
                            List<TypeFact> overThreshold,
                            Map<String, List<MethodFact>> topMethodsPerClass,
                            int maxParameters,
                            List<MethodFact> methodsWithMaxParameters,
                            int methodsWithBody,
                            int bodyLinesTotal) {
}
