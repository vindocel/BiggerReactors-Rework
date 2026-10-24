package biggerreactorsrework.core.util;

/**
 * Functional interfaces whose single method may throw a checked exception.
 */
public final class ThrowingFunctions {
    private ThrowingFunctions() {
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, U, E extends Exception> {
        void accept(T t, U u) throws E;
    }
}
