package biggerreactorsrework.core.energy;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class PowerDistributionTest {
    @Test void tinyBudgetsAreDeliveredWithoutStrandingIntegerRemainders() {
        for (long initial : new long[]{1, 3, 5, 7, 13}) {
            var balance = new AtomicLong(initial);
            var received = new AtomicLong();
            var perPort = new long[4];
            long sent = PowerDistribution.distribute(initial, List.of(0, 1, 2, 3), (port, offer, simulate) -> {
                if (!simulate) { received.addAndGet(offer); perPort[port] += offer; }
                return offer;
            }, amount -> { balance.addAndGet(-amount); return amount; });
            assertEquals(initial, sent);
            assertEquals(initial, received.get());
            assertEquals(0, balance.get());
            long min = java.util.Arrays.stream(perPort).min().orElseThrow();
            long max = java.util.Arrays.stream(perPort).max().orElseThrow();
            assertTrue(max - min <= 1, "equal consumers should share the integer remainder");
        }
    }

    @Test void roundingNeverExceedsTheIndividualRequestNearLongPrecisionLimits() {
        long request = (1L << 53) + 3;
        long stored = request * 2 + 1;
        var balance = new AtomicLong(stored);
        long sent = PowerDistribution.distribute(stored, List.of(0, 1), (port, offer, simulate) -> {
            if (!simulate) assertTrue(offer <= request);
            return Math.min(offer, request);
        }, amount -> { balance.addAndGet(-amount); return amount; });
        assertEquals(request * 2, sent);
        assertEquals(1, balance.get());
    }
    @Test void sharesLargeBatteryAndDebitsOnlyActualAcceptance() {
        var balance = new AtomicLong(8_000_000_000L);
        var received = new long[2];
        long sent = PowerDistribution.distribute(balance.get(), List.of(0, 1), (port, offer, simulate) -> {
            if (simulate) return offer;
            long accepted = port == 0 ? offer / 2 : offer;
            received[port] += accepted;
            return accepted;
        }, amount -> { balance.addAndGet(-amount); return amount; });
        assertEquals(6_000_000_000L, sent);
        assertEquals(sent, received[0] + received[1]);
        assertEquals(8_000_000_000L - sent, balance.get());
    }

    @Test void malformedConsumersCannotMakeAccountingNegativeOrOverflow() {
        var balance = new AtomicLong(Long.MAX_VALUE);
        long sent = PowerDistribution.distribute(balance.get(), List.of(0, 1, 2),
                (port, offered, simulate) -> port == 0 ? -100 : Long.MAX_VALUE,
                amount -> { balance.addAndGet(-amount); return amount; });
        assertTrue(sent > 0);
        assertTrue(balance.get() >= 0);
        assertEquals(Long.MAX_VALUE - sent, balance.get());
    }

    @Test void emptyStorageDoesNotContactConsumers() {
        assertEquals(0, PowerDistribution.distribute(0, List.of(1), (p, a, s) -> { fail(); return 1; }, a -> a));
    }
}
