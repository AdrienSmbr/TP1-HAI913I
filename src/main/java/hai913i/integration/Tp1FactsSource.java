package hai913i.integration;

import hai913i.tp1.extract.FactExtractor;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeKind;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp2.Facts;
import hai913i.tp2.FactsSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Adaptateur direct du modèle TP1 ; le moteur TP2 ne dépend pas de JDT. */
public final class Tp1FactsSource implements FactsSource {
    private final Path project;
    public Tp1FactsSource(Path project) { this.project = Objects.requireNonNull(project); }
    public record Adapted(Facts facts, Map<String, Integer> recoveredTargets) {
        public Adapted { recoveredTargets = Collections.unmodifiableMap(new TreeMap<>(recoveredTargets)); }
    }
    public ProjectFacts extract() throws IOException {
        ProjectSources sources = ProjectSources.of(project);
        if (sources.isEmpty()) throw new IOException("aucun fichier Java sous " + project);
        List<String> classpath = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(s -> Path.of(s).toAbsolutePath().toString()).toList();
        var files = JdtParser.parse(sources, classpath);
        List<String> errors = new ArrayList<>();
        for (var file : files) for (var problem : file.unit().getProblems()) if (problem.isError())
            errors.add(file.path().getFileName() + ":" + problem.getSourceLineNumber() + " " + problem.getMessage());
        if (!errors.isEmpty()) throw new IOException("extraction refusée : " + errors.size()
                + " erreurs de compilation\n" + String.join("\n", errors));
        return FactExtractor.extract(files);
    }
    @Override public Facts load() throws IOException { return adapt(extract()).facts(); }
    public static Adapted adapt(ProjectFacts project) {
        List<String> classes = project.types().stream().map(t -> t.qualifiedName()).sorted().toList();
        Map<String, String> owners = new TreeMap<>();
        Map<String, TypeKind> kinds = new TreeMap<>();
        project.types().forEach(t -> { kinds.put(t.qualifiedName(), t.kind());
            t.methods().forEach(m -> owners.put(m.id(), t.qualifiedName())); });
        Map<String, Integer> recovered = new TreeMap<>();
        for (var call : project.calls()) if (call.resolved() && call.internal()) {
            String target = call.targetId().orElseThrow();
            if (!owners.containsKey(target)) recovered.merge(target, 1, Integer::sum);
        }
        for (String target : recovered.keySet()) {
            String owner = target.substring(0, target.indexOf('#'));
            if (kinds.get(owner) != TypeKind.RECORD && kinds.get(owner) != TypeKind.ENUM)
                throw new IllegalArgumentException("cible interne non déclarée hors record/enum : " + target);
            owners.put(target, owner);
        }
        Map<String, Map<String, Integer>> outgoing = new TreeMap<>();
        for (var call : project.calls()) if (call.resolved() && call.internal()
                && owners.containsKey(call.targetId().orElseThrow())) {
            if (!owners.containsKey(call.callerId())) throw new IllegalArgumentException("appelant absent : " + call.callerId());
            outgoing.computeIfAbsent(call.callerId(), key -> new TreeMap<>())
                    .merge(call.targetId().orElseThrow(), 1, Integer::sum);
        }
        List<Facts.CallArc> calls = new ArrayList<>();
        outgoing.forEach((caller, targets) -> targets.forEach((callee, sites) -> calls.add(new Facts.CallArc(caller, callee, sites))));
        return new Adapted(new Facts(classes, owners, calls), recovered);
    }
}
