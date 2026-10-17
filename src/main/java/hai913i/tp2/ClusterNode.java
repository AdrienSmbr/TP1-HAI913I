package hai913i.tp2;

import java.util.*;

/** Feuille ou fusion ; les membres sont triés et immuables. */
public record ClusterNode(int id, ClusterNode left, ClusterNode right,
                          double similarity, List<String> members) {
    public ClusterNode {
        if (id < 0 || !Double.isFinite(similarity) || similarity < 0 || similarity > 1)
            throw new IllegalArgumentException("identifiant ou similarité invalide");
        members = members.stream().sorted().toList();
        if ((left == null) != (right == null)) throw new IllegalArgumentException("deux fils requis");
        if (members.isEmpty() || new HashSet<>(members).size() != members.size())
            throw new IllegalArgumentException("membres vides ou dupliqués");
        if (left == null && members.size() != 1) throw new IllegalArgumentException("feuille non singleton");
        if (left != null) {
            if (left.id() >= right.id() || right.id() >= id)
                throw new IllegalArgumentException("identifiants attendus : fils gauche < fils droit < parent");
            List<String> union = new ArrayList<>(left.members()); union.addAll(right.members());
            if (!union.stream().sorted().toList().equals(members))
                throw new IllegalArgumentException("les fils doivent partitionner les membres");
        }
    }
    public boolean isLeaf() { return left == null; }
    public static ClusterNode leaf(int id, String name) {
        return new ClusterNode(id, null, null, 0, List.of(name));
    }
    public static ClusterNode merge(int id, ClusterNode a, ClusterNode b, double similarity) {
        if (a.id() > b.id()) { ClusterNode temp = a; a = b; b = temp; }
        List<String> members = new ArrayList<>(a.members()); members.addAll(b.members());
        return new ClusterNode(id, a, b, similarity, members);
    }
}
