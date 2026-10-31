package biggerreactorsrework.core.energy;

import java.util.List;
import java.util.function.LongUnaryOperator;

/** Shared output accounting for generators. */
public final class PowerDistribution {
    // Modified 2026-10-30: allocate integer remainders before executing transfers.
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
        long[] offers = new long[requests.length];
        long planned = 0;
        for (int i = 0; i < offers.length; i++) {
            offers[i] = Math.min(requests[i], bounded(stored - planned, (long) (requests[i] * fraction)));
            planned += offers[i];
        }
        if (fraction < 1) {
            long remainder = stored - planned;
            for (int pass = 0; pass < 2 && remainder > 0; pass++) {
                for (int i = 0; i < offers.length && remainder > 0; i++) {
                    long extra = Math.min(remainder, requests[i] - offers[i]);
                    // Assign ordinary integer remainders evenly; a second pass handles floating-point precision loss.
                    if (pass == 0) extra = Math.min(1, extra);
                    offers[i] += extra;
                    remainder -= extra;
                }
            }
        }
        long delivered = 0;
        for (int i = 0; i < requests.length; i++) {
            long offer = bounded(stored - delivered, offers[i]);
            if (offer == 0) continue;
            long accepted = bounded(offer, transfer.transfer(ports.get(i), offer, false));
            extract.applyAsLong(accepted);
            delivered += accepted;
        }
        return delivered;
    }
}
