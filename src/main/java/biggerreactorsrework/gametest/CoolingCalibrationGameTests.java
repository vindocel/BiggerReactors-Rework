package biggerreactorsrework.gametest;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.calculator.design.TurbineDesign;
import biggerreactorsrework.calculator.simulation.Bench;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;
// Modified 2026-11-02: qualified water comparison with the real turbine below its RPM governor.
@GameTestHolder(BiggerReactors.modid) @PrefixGameTestTemplate(false)
public final class CoolingCalibrationGameTests {
    @GameTest(template="empty_7x7x7",timeoutTicks=400)
    public static void superiorPassiveStaysBelowHalfQualifiedWater(GameTestHelper h) {
        var d=ThermalModelGameTests.checker(3,3,3,false,null);
        var water=FluidTransitionRegistry.liquidTransition(Fluids.WATER);
        var liquid=ReactorCapacity.measure(d,new SimulationConfiguration(293.15,false),water);
        double best=0;
        for(var p:ReactorModeratorRegistry.snapshot().values()) {
            var passive=ReactorCapacity.measure(ThermalModelGameTests.checker(3,3,3,false,p.passiveClass()),new SimulationConfiguration(293.15,true),null);
            if(passive.burn()>0)best=Math.max(best,passive.power()/passive.burn());
        }
        Bench.Point qualified=null;
        for(int coils:new int[]{16,32,64,96,160,240}) {
            var candidate=Bench.turbine(new TurbineDesign(4,10,0,coils,Blocks.IRON_BLOCK,(long)liquid.converted()),TurbineCoilRegistry.getCoilData(Blocks.IRON_BLOCK),water,()->false);
            if(candidate.converged() && candidate.rpm()>0 && candidate.rpm()<2190 && candidate.output()/Math.floor(liquid.converted())>=1) { qualified=candidate;break; }
        }
        h.assertTrue(qualified!=null,"No qualified uncapped water turbine was measured");
        double waterPerFuel=qualified.output()/liquid.burn();
        BiggerReactors.LOGGER.info("Thermal v1 qualified comparison: passive={} RF/mB fuel; water={} RF/mB fuel; flow={} mB/t; RPM={}",best,waterPerFuel,liquid.converted(),qualified.rpm());
        h.assertTrue(best>0 && best<=.5*waterPerFuel,"Superior passive exceeds half qualified water output");h.succeed();
    }
}
