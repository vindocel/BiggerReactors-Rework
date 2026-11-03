package biggerreactorsrework.gametest;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.registries.*;
import biggerreactorsrework.thermal.ThermalModel;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;
// Modified 2026-11-03: Ludicrite-level passive cooling lands 40-45% below best-coil water; the rarest materials beat it.
@GameTestHolder(BiggerReactors.modid) @PrefixGameTestTemplate(false)
public final class CoolingCalibrationGameTests {
    @GameTest(template="empty_7x7x7",timeoutTicks=400)
    public static void superiorPassiveStaysBelowBestCoilWater(GameTestHelper h) {
        var d=ThermalModelGameTests.checker(3,3,3,false,null);
        var liquid=ReactorCapacity.measure(d,new SimulationConfiguration(293.15,false),FluidTransitionRegistry.liquidTransition(Fluids.WATER));
        var passive=ReactorCapacity.measure(ThermalModelGameTests.checker(3,3,3,false,ThermalModel.PassiveClass.SUPERIOR),new SimulationConfiguration(293.15,true),null);
        double bestCoil=TurbineCoilRegistry.snapshot().values().stream().mapToDouble(TurbineCoilRegistry.CoilData::fePerMB).max().orElse(0);
        double waterPerFuel=liquid.converted()/liquid.burn()*bestCoil,passivePerFuel=passive.power()/passive.burn();
        BiggerReactors.LOGGER.info("Passive calibration: passive={} RF/mB fuel; water={} FE/mB fuel; coil={} FE/mB",passivePerFuel,waterPerFuel,bestCoil);
        double ratio=passivePerFuel/waterPerFuel;
        double rare=ratio*ThermalModel.passiveYield(ThermalModel.PassiveClass.SUPERIOR,5)/ThermalModel.passiveYield(ThermalModel.PassiveClass.SUPERIOR,3);
        h.assertTrue(ratio>=.55 && ratio<=.60,"Conductivity-3 passive should give 55-60% of best-coil water, got "+ratio);
        h.assertTrue(rare>1.1 && rare<1.2,"Conductivity-5 passive should beat best-coil water by 10-20%, got "+rare);h.succeed();
    }
}
