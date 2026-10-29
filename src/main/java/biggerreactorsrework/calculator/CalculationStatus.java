package biggerreactorsrework.calculator;

/** Outcome of one calculation; only the first two carry a design that may enter the history. */
public enum CalculationStatus {
    TARGET_MET, BEST_BELOW_TARGET, SEARCH_LIMIT_REACHED, NOT_CONVERGED, INVALID_INPUT, STALE_CATALOG, CANCELLED, BUSY, FAILED;

    public boolean hasDesign() { return this == TARGET_MET || this == BEST_BELOW_TARGET; }
}
