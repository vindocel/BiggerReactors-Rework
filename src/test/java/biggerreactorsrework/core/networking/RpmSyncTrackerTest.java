package biggerreactorsrework.core.networking;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RpmSyncTrackerTest {
    @Test void stableRotorDoesNotResendIdenticalState() {
        var tracker=new RpmSyncTracker();int sent=0;
        for(int tick=0;tick<200;tick++) if(tracker.sample(tick,1200))sent++;
        assertEquals(1,sent);
    }
    @Test void movementIsBoundedAndStopsImmediately() {
        var tracker=new RpmSyncTracker();assertTrue(tracker.sample(0,0));assertTrue(tracker.sample(1,1));
        assertFalse(tracker.sample(2,2));assertTrue(tracker.sample(11,11));assertTrue(tracker.sample(12,0));
        tracker.reset();assertTrue(tracker.sample(13,0));assertFalse(tracker.sample(14,Double.NaN));
    }
}
