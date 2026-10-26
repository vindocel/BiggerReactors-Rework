package biggerreactorsrework.multiblocks.reactor;

import biggerreactorsrework.multiblocks.reactor.simulation.base.PhaseChange;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhaseChangeTest {
    @Test void fractionalHeatIsConservedAcrossTicks() {
        double remainder=0,accepted=0;long converted=0;
        for(int i=0;i<1000;i++) {
            var step=PhaseChange.accept(.07,15,1000,remainder);
            remainder=step.remainder();accepted+=step.accepted();converted+=step.units();
        }
        assertEquals(70,accepted,1e-8);
        assertEquals(accepted,converted*15+remainder,1e-8);
        assertEquals(4,converted);
    }
    @Test void blockedOutletDoesNotAbsorbHeat() {
        var step=PhaseChange.accept(500,15,0,4);
        assertEquals(0,step.accepted());assertEquals(0,step.units());assertEquals(4,step.remainder());
    }
    @Test void availableVolumeCapsTransitionWithoutDiscardingAcceptedHeat() {
        var step=PhaseChange.accept(100000,15,2,0);
        assertEquals(2,step.units());assertEquals(30+step.remainder(),step.accepted(),1e-9);
        assertTrue(step.remainder()<15);
    }
    @Test void malformedEnergyCannotGenerateFluid() {
        for(double energy:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,0})assertEquals(0,PhaseChange.accept(energy,15,100,0).units());
    }
}
