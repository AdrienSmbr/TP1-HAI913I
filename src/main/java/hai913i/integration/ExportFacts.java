package hai913i.integration;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public final class ExportFacts {
    private ExportFacts() {}
    public static void main(String[] args) {
        try {
            if (args.length != 2) throw new IllegalArgumentException("export-facts PROJET_JAVA DOSSIER_SORTIE");
            export(Path.of(args[0]), Path.of(args[1]));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Erreur : " + e.getMessage()); System.exit(2);
        }
    }
    public static void export(Path project, Path output) throws IOException {
        var original = new Tp1FactsSource(project).extract();
        var adapted = Tp1FactsSource.adapt(original); var facts = adapted.facts();
        StringBuilder classes = new StringBuilder("class;simpleName;package\n");
        original.types().stream().sorted(Comparator.comparing(t -> t.qualifiedName())).forEach(t -> classes
                .append(t.qualifiedName()).append(';').append(t.qualifiedName().substring(t.qualifiedName().lastIndexOf('.') + 1))
                .append(';').append(t.packageName()).append('\n'));
        StringBuilder methods = new StringBuilder("method;owner\n"), calls = new StringBuilder("caller;callerClass;callee;calleeClass;sites\n");
        new TreeMap<>(facts.methodOwners()).forEach((id, owner) -> methods.append(id).append(';').append(owner).append('\n'));
        facts.calls().forEach(c -> calls.append(c.caller()).append(';').append(facts.methodOwners().get(c.caller()))
                .append(';').append(c.callee()).append(';').append(facts.methodOwners().get(c.callee()))
                .append(';').append(c.sites()).append('\n'));
        StringBuilder recovered = new StringBuilder("target;sites;owner;kind\n");
        adapted.recoveredTargets().forEach((target, sites) -> {
            String owner = facts.methodOwners().get(target);
            String kind = original.types().stream().filter(t -> t.qualifiedName().equals(owner)).findFirst().orElseThrow().kind().name();
            recovered.append(target).append(';').append(sites).append(';').append(owner).append(';').append(kind).append('\n');
        });
        Files.createDirectories(output);
        Files.writeString(output.resolve("facts-extracted-classes.csv"), classes);
        Files.writeString(output.resolve("facts-extracted-methods.csv"), methods);
        Files.writeString(output.resolve("facts-extracted-calls.csv"), calls);
        Files.writeString(output.resolve("cibles-reintegrees.csv"), recovered);
        String summary = "Extracteur : TP1 JDT, adaptateur partagé CLI/GUI/export\nClasses : " + facts.classes().size()
                + "\nMéthodes : " + facts.methodOwners().size() + "\nArcs : " + facts.calls().size()
                + "\nSites internes : " + facts.calls().stream().mapToInt(c -> c.sites()).sum()
                + "\nMéthodes implicites cibles ajoutées : " + adapted.recoveredTargets().size()
                + "\nSites réintégrés : " + adapted.recoveredTargets().values().stream().mapToInt(Integer::intValue).sum() + "\n";
        Files.writeString(output.resolve("extraction.txt"), summary); System.out.print(summary);
    }
}
