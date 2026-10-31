package biggerreactorsrework.calculator.service;

// Modified 2026-10-30: exercise request flooding, independent players and reconnect cleanup.

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RequestThrottleTest {
    @Test void rejectedRequestsDoNotExtendTheCooldown() {
        var throttle = new RequestThrottle(1000);
        var player = UUID.randomUUID();
        assertTrue(throttle.acquire(player, 0));
        for (long t = 1; t < 1_000_000_000L; t += 10_000_000L) assertFalse(throttle.acquire(player, t));
        assertTrue(throttle.acquire(player, 1_000_000_000L));
    }

    @Test void differentPlayersAndReconnectionsHaveSeparateBudgets() {
        var throttle = new RequestThrottle(1000);
        var player = UUID.randomUUID();
        assertTrue(throttle.acquire(player, 0));
        assertTrue(throttle.acquire(UUID.randomUUID(), 0));
        throttle.remove(player);
        assertTrue(throttle.acquire(player, 1));
        throttle.clear();
        assertTrue(throttle.acquire(player, 2));
    }
}
