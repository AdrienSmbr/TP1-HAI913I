package hai913i.tp1.metrics;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class Ranking {

    private Ranking() {
    }

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
