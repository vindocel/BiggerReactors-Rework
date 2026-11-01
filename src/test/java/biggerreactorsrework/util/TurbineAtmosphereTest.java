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
    @Test void exposedNonRotorUndersidesCanDripInAnyOrientation() {
        assertTrue(TurbineAtmosphere.condensationSurface(true, false, true));
        assertFalse(TurbineAtmosphere.condensationSurface(true, false, false));
        assertFalse(TurbineAtmosphere.condensationSurface(false, false, true));
        assertFalse(TurbineAtmosphere.condensationSurface(true, true, true));
        assertEquals(1F, TurbineAtmosphere.fade(0.9F, 1F, 1F));
        assertEquals(0.4F, TurbineAtmosphere.fade(0F, 1F, 1F), 1E-6F);
        assertEquals(0.6F, TurbineAtmosphere.fade(1F, 0F, 1F), 1E-6F);
        assertEquals(0F, TurbineAtmosphere.fade(0F, 0F, 0.05F));
    }
    @Test void shaderOpacityKeepsFaintestTextureTexelsAboveCutoutAndEmptyClear() {
        assertEquals(0, TurbineAtmosphere.shaderOpacity(0));
        assertEquals(0, TurbineAtmosphere.shaderOpacity(Float.NaN));
        assertEquals(0, TurbineAtmosphere.shaderOpacity(Float.POSITIVE_INFINITY));
        assertEquals(52, TurbineAtmosphere.shaderOpacity(1));
        for (int alpha = 1; alpha <= 32; alpha++) {
            int shaderAlpha = TurbineAtmosphere.shaderOpacity(alpha / 32F);
            assertTrue((shaderAlpha / 255F) * (162 / 255F) > 0.1F);
        }
        assertEquals(32, TurbineAtmosphere.opacity(1));
    }
}
