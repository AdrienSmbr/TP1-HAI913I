package hai913i.tp1.callgraph;

import java.util.List;
import java.util.Map;

/**
 * Graphe d'appel du projet, semantique de la section 4.4 du sujet.
 *
 * <p>Noeuds : les methodes comptees du projet, identifiees par un nom stable et
 * unique ; les surcharges sont donc des noeuds distincts. Arc f -> g : le corps
 * de f contient au moins un appel dont la cible resolue est g. Plusieurs sites
 * d'appel de f vers g donnent un seul arc, dont le poids est le nombre de sites.
 * La recursion donne un arc f -> f. Les appels vers l'exterieur du projet et les
 * appels non resolus ne produisent pas d'arc : ils sont comptes a part.</p>
 */
public record CallGraph(List<String> nodes,
                        Map<String, Map<String, Integer>> outgoing,
                        Map<String, Map<String, Integer>> incoming,
                        int externalCalls,
                        int unresolvedCalls) {

    public int nodeCount() {
        return nodes.size();
    }

    /** Nombre d'arcs : une paire (f, g) distincte, quel que soit son poids. */
    public int arcCount() {
        return outgoing.values().stream().mapToInt(Map::size).sum();
    }

    /** Nombre de sites d'appel internes : la somme des poids. */
    public int siteCount() {
        return outgoing.values().stream()
                .flatMap(targets -> targets.values().stream())
                .mapToInt(Integer::intValue)
                .sum();
    }

    /** Methodes appelees par la methode donnee, avec le poids de chaque arc. */
    public Map<String, Integer> callees(String methodId) {
        return outgoing.getOrDefault(methodId, Map.of());
    }

    /** Methodes appelantes de la methode donnee : le graphe inverse. */
    public Map<String, Integer> callers(String methodId) {
        return incoming.getOrDefault(methodId, Map.of());
    }

    public boolean contains(String methodId) {
        return nodes.contains(methodId);
    }
}
