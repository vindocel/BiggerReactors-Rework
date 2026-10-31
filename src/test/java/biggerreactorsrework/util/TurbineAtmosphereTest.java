package biggerreactorsrework.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TurbineAtmosphereTest {
    @Test void emptyAndUnconfiguredTanksHaveNoHaze() {
        assertEquals(0, TurbineAtmosphere.density(0, 1000));
        assertEquals(0, TurbineAtmosphere.density(-1, 1000));
        assertEquals(0, TurbineAtmosphere.density(1000, 0));
    }
    @Test void storedSteamAloneSetsBoundedDensity() {
        float previous = 0;
        for (int stored = 0; stored <= 1200; stored++) {
            float next = TurbineAtmosphere.density(stored, 1000);
            assertTrue(Float.isFinite(next) && next >= previous && next >= 0 && next <= 1);
            previous = next;
        }
    }
    @Test void condensationRequiresThreeSecondsOfContinuousRotatingOperation() {
        var timer = new TurbineAtmosphere.CondensationTimer();
        for(int i=0;i<59;i++)assertFalse(timer.tick(true,10,1.1));
        assertTrue(timer.tick(true,10,1.1));
        assertTrue(timer.tick(true,10,1.1));
        assertFalse(timer.tick(false,10,500));
        for(int i=0;i<59;i++)assertFalse(timer.tick(true,10,1.1));
        assertFalse(timer.tick(true,0,500));
        assertFalse(timer.tick(true,10,0));
        assertFalse(timer.tick(true,10,Double.NaN));
        assertFalse(timer.tick(true,10,Double.POSITIVE_INFINITY));
        timer.reset();assertFalse(timer.ready());
    }
    @Test void fullyConsumedThroughFlowRemainsVisibleWithoutStoredSteam() {
        assertEquals(1, TurbineAtmosphere.density(0, 1000000, true, 10000, 10000));
        assertEquals(.5F, TurbineAtmosphere.density(0, 1000000, true, 2500, 10000));
        assertEquals(0, TurbineAtmosphere.density(0, 1000000, true, 0, 10000));
        assertEquals(0, TurbineAtmosphere.density(0, 1000000, false, 10000, 10000));
        assertEquals(.5F, TurbineAtmosphere.density(250000, 1000000, false, 10000, 10000));
        assertEquals(.5F, TurbineAtmosphere.density(250000, 1000000, true, 1, 10000));
        assertEquals(1, TurbineAtmosphere.density(1000000, 1000000, true, 10000, 10000));
        assertEquals(0, TurbineAtmosphere.density(0, 1000000, true, 10000, 0));
        assertEquals(0, TurbineAtmosphere.density(0, 1000000, true, -1, 10000));
        assertEquals(1, TurbineAtmosphere.density(0, 1000000, true, Long.MAX_VALUE, 10000));
    }
    @Test void maximumOpacityIsTwentyEightPercentStrongerAndStillBounded() {
        assertEquals(32, TurbineAtmosphere.opacity(1));
        assertEquals(1.28, TurbineAtmosphere.opacity(1) / 25D);
        assertEquals(0, TurbineAtmosphere.opacity(0));
        assertEquals(0, TurbineAtmosphere.opacity(-1));
        assertEquals(32, TurbineAtmosphere.opacity(2));
        assertEquals(0, TurbineAtmosphere.opacity(Float.NaN));
        assertEquals(0, TurbineAtmosphere.opacity(Float.POSITIVE_INFINITY));
    }
    @Test void OnlyExposedUpperCeilingAndCoilUndersidesCanDrip() {
        assertTrue(TurbineAtmosphere.condensationSurface(true, false, true, 10, 0, 10));
        assertTrue(TurbineAtmosphere.condensationSurface(false, true, true, 8, 0, 10));
        assertFalse(TurbineAtmosphere.condensationSurface(true, true, false, 8, 0, 10));
        assertFalse(TurbineAtmosphere.condensationSurface(false, true, true, 2, 0, 10));
        assertFalse(TurbineAtmosphere.condensationSurface(false, false, true, 8, 0, 10));
    }
}
