package biggerreactorsrework.calculator.search;

import biggerreactorsrework.calculator.design.TurbineDesign;
import biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

/**
 * A compact turbine reaching the target. Modified 2026-11-04: tune flow against the same speed efficiency curve
 * used by the assembled turbine.
 * Spacer layers make it at least {@code minShaft} tall. A rotor shorter than three blocks above its width gets extra
 * blade layers to reach that look; a taller one stays as it is.
 */
final class TurbineSearch {
    record Choice(@Nullable TurbineDesign design, double power, int benches) { }

    private TurbineSearch() { }

    static Choice design(long target, Block coil, TurbineCoilRegistry.CoilData data, FluidTransitionRegistry.FluidTransition steam,
                         int maxArm, int maxShaft, int minShaft, int minArm, BooleanSupplier cancelled) {
        double perMB = data.fePerMB() * ModernTurbineSimulation.steamFactor(steam);
        if (perMB <= 0 || data.steamPerTick() <= 0) return new Choice(null, 0, 0);
        long flow = (long) Math.ceil(target / perMB);
        TurbineDesign best = null;
        double bestPower = 0;
        int benches = 0;
        for (int arm = Math.max(1, minArm); arm <= maxArm; arm++) {
            if (cancelled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
            var layer = new TurbineDesign(arm, 1, 0, 0, coil, 0);
            long size = Math.min(flow, layer.flowLimit());
            int blades = (int) Math.max(1, Math.ceil(size / layer.bladeCapacity(ModernTurbineSimulation.NOMINAL_RPM)));
            int coils = (int) Math.max(1, (size + data.steamPerTick() - 1) / data.steamPerTick());
            var design = new TurbineDesign(arm, blades, 0, coils, coil, size);
            design = new TurbineDesign(arm, blades, Math.max(0, minShaft - design.shaft()), coils, coil, size);
            if (design.shaft() > maxShaft) continue;
            benches++;
            var sim = design.simulation(data);
            sim.idealSupply(steam);
            long tuned = tuneFlow(sim, size, target);
            design = design.withFlow(tuned);
            double power = sim.steadyPower(tuned);
            if (best == null || better(design, power, best, bestPower, target)) {
                best = design;
                bestPower = power;
            }
        }
        // Modified 2026-11-03: the preferred look (three blocks taller than wide) never widens the most compact rotor;
        // a shorter rotor reaches it with extra blade layers, which only add spare blade capacity, never empty layers.
        if (best != null && bestPower >= target) {
            int preferredShaft = best.width() + 3 - 2;
            if (best.shaft() < preferredShaft && preferredShaft <= maxShaft) {
                var shaped = new TurbineDesign(best.arm(), best.bladeLayers() + preferredShaft - best.shaft(), best.spacer(), best.coils(), coil, best.flow());
                var sim = shaped.simulation(data);
                sim.idealSupply(steam);
                long tuned = tuneFlow(sim, best.flow(), target);
                double power = sim.steadyPower(tuned);
                if (power >= target) return new Choice(shaped.withFlow(tuned), power, benches + 1);
            }
        }
        return new Choice(best, bestPower, benches);
    }

    // Seek the upper efficiency peak when the raw coil rate falls short.
    private static long tuneFlow(ModernTurbineSimulation sim, long initial, long target) {
        long lower = Math.min(initial, sim.flowRateLimit());
        if (sim.steadyPower(lower) >= target) return lower;
        double rpmPerMB = sim.steadyRPM(1);
        if (rpmPerMB <= 0) return lower;
        long peak = Math.min(sim.flowRateLimit(), Math.round(ModernTurbineSimulation.NOMINAL_RPM / rpmPerMB));
        if (sim.steadyPower(peak) < target) return sim.steadyPower(peak) > sim.steadyPower(lower) ? peak : lower;
        long upper = peak;
        lower = Math.min(upper, Math.max(lower, (long) Math.ceil(peak / Math.sqrt(2))));
        while (lower < upper) {
            long middle = lower + (upper - lower) / 2;
            if (sim.steadyPower(middle) >= target) upper = middle;
            else lower = middle + 1;
        }
        return lower;
    }

    private static boolean better(TurbineDesign a, double ap, TurbineDesign b, double bp, long target) {
        boolean am = ap >= target, bm = bp >= target;
        if (am != bm) return am;
        if (!am) return ap > bp;
        int size = Compactness.compare(a.width(), a.height(), a.depth(), b.width(), b.height(), b.depth());
        return size < 0 || size == 0 && a.flow() < b.flow();
    }
}
