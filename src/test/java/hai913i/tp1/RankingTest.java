package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import hai913i.tp1.metrics.Ranking;

class RankingTest {

    private record Candidate(String name, int value) {
    }

    private static List<Candidate> of(int... values) {
        return java.util.stream.IntStream.range(0, values.length)
                .mapToObj(i -> new Candidate(String.format("c%02d", i), values[i]))
                .toList();
    }

    private static List<String> top(List<Candidate> candidates) {
        return Ranking.topTenPercent(candidates, Candidate::value, Candidate::name)
                .stream().map(Candidate::name).toList();
    }

    @Test
    @DisplayName("k vaut le plafond de n/10, pas l'arrondi vers le bas")
    void quotaIsCeiling() {
        assertEquals(0, Ranking.quota(0));
        assertEquals(1, Ranking.quota(1));
        assertEquals(2, Ranking.quota(15), "(int) (15 * 0.1) donnerait 1");
        assertEquals(1, Ranking.quota(10));
        assertEquals(2, Ranking.quota(11));
    }

    @Test
    @DisplayName("aucun candidat : resultat vide")
    void emptyCandidates() {
        assertTrue(top(List.of()).isEmpty());
    }

    @Test
    @DisplayName("au moins un retenu des que n >= 1")
    void atLeastOne() {
        assertEquals(List.of("c00"), top(of(7)));
    }

    @Test
    @DisplayName("les ex aequo du k-ieme sont retenus, le resultat peut depasser k")
    void tiesAreKept() {
        // n = 15 donc k = 2 ; le 2e vaut 6, les deux autres 6 sont pris aussi
        List<Candidate> candidates = of(10, 6, 6, 6, 5, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1);
        assertEquals(List.of("c00", "c01", "c02", "c03"), top(candidates));
    }

    @Test
    @DisplayName("une valeur nulle n'est jamais retenue")
    void zeroIsNeverKept() {
        assertTrue(top(of(0, 0, 0, 0, 0)).isEmpty());
    }

    @Test
    @DisplayName("les zeros sont ecartes meme parmi les k premiers")
    void zeroDroppedInsideQuota() {
        assertEquals(List.of("c00"), top(of(3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    }
}
