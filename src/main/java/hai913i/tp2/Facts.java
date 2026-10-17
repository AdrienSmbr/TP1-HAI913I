package hai913i.tp2;

import java.util.*;

/** Faits neutres, indépendants du chargeur et de toute bibliothèque d'AST. */
public record Facts(List<String> classes, Map<String, String> methodOwners, List<CallArc> calls) {
    public record CallArc(String caller, String callee, int sites) {}

    public Facts {
        classes = classes.stream().sorted().toList();
        methodOwners = Map.copyOf(methodOwners);
        calls = List.copyOf(calls);
        Set<String> names = new HashSet<>(classes);
        if (classes.isEmpty() || names.size() != classes.size())
            throw new IllegalArgumentException("classes vides ou dupliquées");
        for (String name : classes) {
            Csv.field(name);
        }
        for (var entry : methodOwners.entrySet()) {
            Csv.field(entry.getKey());
            if (entry.getKey().isBlank() || !names.contains(entry.getValue()))
                throw new IllegalArgumentException("propriétaire de méthode inconnu : " + entry);
        }
        Set<List<String>> unique = new HashSet<>();
        for (CallArc arc : calls) {
            if (!methodOwners.containsKey(arc.caller()) || !methodOwners.containsKey(arc.callee()))
                throw new IllegalArgumentException("méthode inconnue dans l'arc : " + arc);
            if (arc.sites() < 1) throw new IllegalArgumentException("sites doit être positif");
            if (!unique.add(List.of(arc.caller(), arc.callee())))
                throw new IllegalArgumentException("arc dupliqué : " + arc);
        }
    }
}
