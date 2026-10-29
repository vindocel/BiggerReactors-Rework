package biggerreactorsrework.calculator.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactnessTest {
    @Test
    void smallerVolumeWinsEvenWhenItsAspectRatioIsLonger() {
        assertTrue(Compactness.compare(25, 3, 3, 7, 7, 7) < 0);
    }

    @Test
    void equalVolumePrefersBalancedDimensionsWithoutAForbiddenRatio() {
        assertTrue(Compactness.compare(8, 8, 8, 32, 4, 4) < 0);
        assertTrue(Compactness.compare(10, 10, 20, 3, 3, 200) < 0);
        assertTrue(Compactness.compare(3, 3, 200, 100, 100, 100) < 0);
    }

    @Test
    void fifteenMillionCasePrefersWiderShorterTurbineEvenWithSlightlyMoreVolume() {
        assertTrue(Compactness.compare(23, 34, 23, 17, 59, 17) < 0);
    }
}
