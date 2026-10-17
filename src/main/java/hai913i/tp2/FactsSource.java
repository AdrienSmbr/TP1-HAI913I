package hai913i.tp2;

import java.io.IOException;

/** Frontière d'entrée : un autre adaptateur peut fournir les faits du TP1. */
@FunctionalInterface
public interface FactsSource {
    Facts load() throws IOException;
}
