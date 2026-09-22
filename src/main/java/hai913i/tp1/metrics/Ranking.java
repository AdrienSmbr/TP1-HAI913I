package hai913i.tp1.metrics;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * La regle des « 10 % » du sujet, section 4.3.
 *
 * <p>Parmi n candidats, on retient les k = plafond(n/10) premiers dans l'ordre
 * decroissant de la valeur mesuree. Tout candidat dont la valeur egale celle du
 * k-ieme est retenu aussi : le resultat peut donc depasser k elements. Un
 * candidat dont la valeur vaut 0 n'est jamais retenu.</p>
 */
public final class Ranking {

    private Ranking() {
    }

    /** Plafond de n/10 en entiers. (int) (n * 0.1) arrondirait vers le bas. */
    public static int quota(int candidateCount) {
        return (candidateCount + 9) / 10;
    }

    public static <T> List<T> topTenPercent(List<T> candidates,
            ToIntFunction<T> value,
            Function<T, String> name) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<T> sorted = candidates.stream()
                .sorted(Comparator.comparingInt(value).reversed()
                        .thenComparing(name))
                .toList();
        int cut = value.applyAsInt(sorted.get(quota(candidates.size()) - 1));
        return sorted.stream()
                .filter(candidate -> value.applyAsInt(candidate) >= cut)
                .filter(candidate -> value.applyAsInt(candidate) > 0)
                .toList();
    }
}
