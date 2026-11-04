package biggerreactorsrework.thermal;

import biggerreactorsrework.multiblocks.turbine.simulation.CoilCoordinates;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Added 2026-11-02: translations and axis reversal must not change a coil's radial distance.
class CoilCoordinatesTest {
    @Test void everyAxisPreservesDistancesAtWorldLimits() {
        for(int base:new int[]{0,10000000,-10000000,29999000,-29999000})for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}) {
            int[] delta={sign*3,sign*3,sign*3};delta[axis]=sign*27;
            var radial=CoilCoordinates.relative(base+delta[0],base+delta[1],base+delta[2],base,base,base,axis);
            assertEquals(3,Math.max(Math.abs(radial.first()),Math.abs(radial.second())));
        }
    }
}
