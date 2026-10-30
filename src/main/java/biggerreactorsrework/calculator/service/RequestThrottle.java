package biggerreactorsrework.calculator.service;

// Modified 2026-10-30: bound request frequency independently for each connection operation.

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class RequestThrottle {
    private final Map<UUID, Long> acceptedAt = new HashMap<>();
    private final long intervalNanos;

    RequestThrottle(long intervalMillis) {
        intervalNanos = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(intervalMillis);
    }

    boolean acquire(UUID player, long now) {
        Long last = acceptedAt.get(player);
        if (last != null && now - last < intervalNanos) return false;
        acceptedAt.put(player, now);
        return true;
    }

    void remove(UUID player) { acceptedAt.remove(player); }
    void clear() { acceptedAt.clear(); }
}
