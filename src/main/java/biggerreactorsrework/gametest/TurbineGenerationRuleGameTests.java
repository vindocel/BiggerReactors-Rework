package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.calculator.design.TurbineDesign;
import biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

// Added 2026-11-03: turbine output is the handled steam times the coil material's FE per mB.
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

        var sim = new TurbineDesign(1, 3, 0, 2, Blocks.IRON_BLOCK, 500).simulation(iron);
        run(sim, 1000);
        h.assertTrue(sim.RPM() == 900 && sim.steadyPower(500) == 1375, "900 RPM peak changed");
        sim.setNominalFlowRate(707);
        run(sim, 1000);
        h.assertTrue(sim.RPM() > 1200 && sim.RPM() < 1300
                && sim.bladeEfficiencyLastTick() < 0.51, "efficiency trough missing");
        sim.setNominalFlowRate(1000);
        h.assertTrue(run(sim, 1000) == 2750 && sim.RPM() == 1800, "1800 RPM peak changed");
        sim.setNominalFlowRate(sim.flowRateLimit());
        long overspeed = run(sim, 1000);
        h.assertTrue(sim.RPM() == 2200 && overspeed > 0 && overspeed < 2750,
                "rotor must overspeed with lower generation and stop at the governor");

        // Modified 2026-11-03: excess supply stays in the inlet at the governor, and reduced supply lowers speed.
        var steam = FluidTransitionRegistry.liquidTransition(Fluids.WATER);
        long intakeLimit = sim.flowRateLimit();
        sim.fluidTank().dumpVapor();
        sim.fluidTank().dumpLiquid();
        long supplied = sim.fluidTank().fill(steam.gases.getFirst(), net.minecraft.core.component.DataComponentPatch.EMPTY,
                intakeLimit + 500, false);
        h.assertTrue(supplied == intakeLimit + 500, "test did not supply excess steam");
        sim.tick();
        h.assertTrue(sim.RPM() == 2200 && sim.flowLastTick() == intakeLimit
                && sim.fluidTank().vaporAmount() == 500, "governor consumed excess steam");
        sim.tick();
        h.assertTrue(sim.nominalFlowRate() == intakeLimit && sim.flowLastTick() == 500 && sim.RPM() < 2200,
                "reduced supply did not slow the rotor with the same flow setting");

        var golden = new TurbineDesign(1, 3, 0, 2, Blocks.GOLD_BLOCK, 875).simulation(gold);
        long goldOutput = run(golden, 1000);
        h.assertTrue(golden.RPM() == 900 && Math.abs(goldOutput - 4593.75) < 1,
                "gold rate changed at the 900 RPM peak");
        h.succeed();
    }

    private TurbineGenerationRuleGameTests() { }
}
