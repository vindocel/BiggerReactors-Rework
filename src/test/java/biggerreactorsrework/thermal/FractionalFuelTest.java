package biggerreactorsrework.thermal;

import biggerreactorsrework.core.serialization.DataCompound;
import biggerreactorsrework.multiblocks.reactor.simulation.base.FuelTank;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Added 2026-11-02: prevent unpaid fuel extraction and an immortal final residue.
class FractionalFuelTest {
    @Test void partiallyUsedUnitCannotBeExtractedAsFreshFuel() {
        var tank=new FuelTank(4000);tank.insertFuel(10,false);tank.burn(.25);
        assertEquals(9.75,tank.usableFuel());assertEquals(9,tank.extractFuel(Long.MAX_VALUE,true));
        assertEquals(9,tank.extractFuel(Long.MAX_VALUE,false));assertEquals(.75,tank.usableFuel());
        var restored=new FuelTank(1);restored.load(tank.save());assertEquals(.75,restored.usableFuel());
        restored.burn(1);assertEquals(.75,restored.burnedLastTick());assertEquals(0,restored.fuel());assertEquals(1,restored.waste());
    }
    @Test void precisionConsumesTheFinalResidueWithoutRoundingFuelUp() {
        var n=new DataCompound();n.put("fuel",1L);n.put("partialUsed",1-1e-12);
        var tank=new FuelTank(4000);tank.load(n);double available=tank.usableFuel();
        tank.burn(ThermalModel.Preset.DEFAULT.requestedBurn(available,1,1));
        assertEquals(available,tank.burnedLastTick());assertEquals(0,tank.usableFuel());assertEquals(0,tank.fuel());
        tank.burn(Double.NaN);assertEquals(0,tank.burnedLastTick());tank.burn(-2);assertEquals(0,tank.usableFuel());
    }
}
