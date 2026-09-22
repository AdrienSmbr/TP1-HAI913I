package hai913i.tp1.cli;

public enum Action {
    STRUCTURE, CALLS, METRICS, GRAPH, SPOON, ALL;

    public boolean needsThreshold() {
        return this == METRICS || this == ALL;
    }
}
