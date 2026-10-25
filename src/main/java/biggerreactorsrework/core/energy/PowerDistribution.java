package biggerreactorsrework.core.energy;

import java.util.List;
import java.util.function.LongUnaryOperator;

/** Shared output accounting for generators. */
public final class PowerDistribution {
    private PowerDistribution() { }

    @FunctionalInterface
    public interface Port<T> {
        long transfer(T port, long offered, boolean simulate);
    }

    public static long bounded(long offered, long reported) {
        return Math.max(0, Math.min(Math.max(0, offered), reported));
    }

    public static <T> long distribute(long stored, java.util.Collection<T> source, Port<T> transfer, LongUnaryOperator extract) {
        if (stored <= 0 || source.isEmpty()) return 0;
        List<T> ports = List.copyOf(source);
        long[] requests = new long[ports.size()];
        double total = 0;
        for (int i = 0; i < requests.length; i++) {
            requests[i] = bounded(stored, transfer.transfer(ports.get(i), stored, true));
            total += requests[i];
        }
        if (total <= 0) return 0;
        double fraction = Math.min(1, stored / total);
        long delivered = 0;
        for (int i = 0; i < requests.length; i++) {
            long offer = bounded(stored - delivered, (long) (requests[i] * fraction));
            if (offer == 0) continue;
            long accepted = bounded(offer, transfer.transfer(ports.get(i), offer, false));
            extract.applyAsLong(accepted);
            delivered += accepted;
        }
        return delivered;
    }
}
