package biggerreactorsrework.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectedGlassCornersTest {
    @Test void aSolidBlockSurroundedByGlassHasFourInwardCorners() {
        assertEquals(1, ConnectedGlassCorners.mask(15, 14));
        assertEquals(2, ConnectedGlassCorners.mask(15, 13));
        assertEquals(4, ConnectedGlassCorners.mask(15, 11));
        assertEquals(8, ConnectedGlassCorners.mask(15, 7));
    }

    @Test void uninterruptedGlassAndIsolatedGlassNeedNoInwardCorners() {
        assertEquals(0, ConnectedGlassCorners.mask(15, 15));
        assertEquals(0, ConnectedGlassCorners.mask(0, 0));
        assertEquals(0, ConnectedGlassCorners.mask(3, 0)); // horizontal strip
        assertEquals(0, ConnectedGlassCorners.mask(12, 0)); // vertical strip
    }

    @Test void cornersAlsoWorkAtAnLShapedWindowEdge() {
        assertEquals(1, ConnectedGlassCorners.mask(5, 0));
        assertEquals(2, ConnectedGlassCorners.mask(6, 0));
        assertEquals(4, ConnectedGlassCorners.mask(9, 0));
        assertEquals(8, ConnectedGlassCorners.mask(10, 0));
        assertEquals(0, ConnectedGlassCorners.mask(5, 1));
    }

    @Test void separateOpeningsCanMeetAtTheSameGlassBlock() {
        assertEquals(15, ConnectedGlassCorners.mask(15, 0));
        assertEquals(9, ConnectedGlassCorners.mask(15, 6));
        assertEquals(3, ConnectedGlassCorners.mask(7, 0));
    }
}
