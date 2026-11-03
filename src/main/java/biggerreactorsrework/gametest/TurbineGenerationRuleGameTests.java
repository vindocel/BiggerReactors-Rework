package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.calculator.design.TurbineDesign;
import biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// Added 2026-11-03: turbine output is the handled steam times the coil material's FE per mB.
@GameTestHolder(BiggerReactors.modid) @PrefixGameTestTemplate(false)
public final class TurbineGenerationRuleGameTests {
    private static long run(ModernTurbineSimulation sim, int ticks) {
        var steam = FluidTransitionRegistry.liquidTransition(Fluids.WATER);
        long last = 0;
        for (int tick = 0; tick < ticks; tick++) {
            sim.idealSupply(steam);
            sim.tick();
            last = sim.battery().extract(Long.MAX_VALUE);
        }
        return last;
    }

    @GameTest(template="empty_7x7x7")
    public static void outputIsHandledSteamTimesCoilRate(GameTestHelper h) {
        var iron = new TurbineCoilRegistry.CoilData(.33, 1, .1);
        var gold = new TurbineCoilRegistry.CoilData(.66, 1, .175);
        h.assertTrue(iron.fePerMB() == 2.75 && iron.steamPerTick() == 500, "iron rule changed");
        h.assertTrue(gold.fePerMB() == 5.25 && gold.steamPerTick() == 875, "gold rule changed");
        h.assertTrue(new TurbineCoilRegistry.CoilData(1.15, 1.02, .35).fePerMB() == 9.25, "ludicrite rule changed");

        var within = new TurbineDesign(1, 3, 0, 4, Blocks.IRON_BLOCK, 300).simulation(iron);
        h.assertTrue(within.bladeCapacity() > 400, "reference rotor too small");
        long first = run(within, 1);
        h.assertTrue(first < 825, "start-up generated full power before spinning up");
        h.assertTrue(run(within, 1000) == 825 && within.RPM() == ModernTurbineSimulation.NOMINAL_RPM, "300 mB/t of iron coils did not give 825 FE/t");

        var coilLimited = new TurbineDesign(1, 3, 0, 1, Blocks.IRON_BLOCK, 1000).simulation(iron);
        h.assertTrue(run(coilLimited, 1000) == 1375, "one iron coil did not cap at 500 mB/t");

        var golden = new TurbineDesign(1, 3, 0, 6, Blocks.GOLD_BLOCK, 1000).simulation(gold);
        h.assertTrue(run(golden, 1000) == 5250 && golden.steadyPower(1000) == 5250, "gold example did not give 5,250 FE/t");

        var bladeLimited = new TurbineDesign(1, 1, 0, 40, Blocks.GOLD_BLOCK, 20000).simulation(gold);
        long expected = (long) Math.floor(bladeLimited.bladeCapacity() * 5.25);
        h.assertTrue(bladeLimited.bladeCapacity() < bladeLimited.coilCapacity() && bladeLimited.flowRateLimit() == (long) bladeLimited.bladeCapacity(), "flow limit is not the usable steam");
        h.assertTrue(Math.abs(run(bladeLimited, 1000) - expected) <= 6, "blades did not limit the handled steam");
        h.succeed();
    }

    private TurbineGenerationRuleGameTests() { }
}
