package biggerreactorsrework.core.util;

// Modified 2026-10-30: tolerate malformed saved controls without changing valid values.

public final class SavedValues {
    private SavedValues() { }

    public static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException | NullPointerException invalid) { return fallback; }
    }

    public static double nonNegative(double value, double fallback) {
        return Double.isFinite(value) && value >= 0 ? value : fallback;
    }
}
