package biggerreactorsrework.calculator.search;

import biggerreactorsrework.Config;
import biggerreactorsrework.calculator.design.TurbineDesign;
import biggerreactorsrework.calculator.simulation.Bench;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A compact turbine reaching the target, with an active preference for balanced dimensions. Generation follows the speed efficiency curve, which
 * has several peaks (1800 and 900 RPM with the default configuration), so power is not monotonic in the flow. For
 * each peak the coil count that delivers the target there is estimated from the rotor's energy balance; the flow is
 * then searched only between the valley below the peak and just above it, where power grows with flow, using the
 * real turbine simulation fed its nominal flow. Spacer layers make it at least {@code minShaft} tall.
 */
final class TurbineSearch {
    record Choice(@Nullable TurbineDesign design, double power, int benches) { }

    private TurbineSearch() { }

    static Choice design(long target, Block coil, TurbineCoilRegistry.CoilData data, FluidTransitionRegistry.FluidTransition steam,
                         int maxArm, int maxShaft, int minShaft, int minArm, BooleanSupplier cancelled) {
        var run = new Run(target, coil, data, steam, maxShaft, minShaft, cancelled);
        var found = new java.util.ArrayList<Point>();
        Point best = null;
        for (double peak : run.peaks())
            for (int arm = Math.max(1, minArm); arm <= maxArm; arm++) {
                for (var point : run.forArm(arm, peak)) {
                    found.add(point);
                    if (best == null || run.better(point, best)) best = point;
                }
            }
        if (best == null) return new Choice(null, 0, run.benches);
        if (best.power < target) return new Choice(best.design, best.power, run.benches);
        // Modified 2026-10-29: size first, without excluding a compact rotor by an arbitrary steam allowance.
        for (var point : found)
            if (point.power >= target) {
                int size = Compactness.compare(point.design.width(), point.design.height(), point.design.depth(),
                        best.design.width(), best.design.height(), best.design.depth());
                if (size < 0 || size == 0 && point.design.flow() < best.design.flow()) best = point;
            }
        return new Choice(best.design, best.power, run.benches);
    }

    private static long blocks(TurbineDesign d) {
        return d.blades() + d.coils() + d.shaft();
    }

    private record Point(TurbineDesign design, double power) { }

    private static final class Run {
        final long target;
        final Block coil;
        final TurbineCoilRegistry.CoilData data;
        final FluidTransitionRegistry.FluidTransition steam;
        final int maxShaft, minShaft;
        final BooleanSupplier cancelled;
        int benches;

        Run(long target, Block coil, TurbineCoilRegistry.CoilData data, FluidTransitionRegistry.FluidTransition steam, int maxShaft, int minShaft, BooleanSupplier cancelled) {
            this.target = target;
            this.coil = coil;
            this.data = data;
            this.steam = steam;
            this.maxShaft = maxShaft;
            this.minShaft = minShaft;
            this.cancelled = cancelled;
        }

        /** Speeds of full efficiency in {@code ModernTurbineSimulation.tick}: the grid peak and its halvings above the low limit. */
        List<Double> peaks() {
            var c = Config.CONFIG.Turbine;
            double peak = c.EffectiveGridFrequency * 60.0, min = peak / Math.pow(2, c.EfficiencyPeaks - 0.5);
            var peaks = new ArrayList<Double>();
            for (double rpm = peak; rpm > min; rpm /= 2) peaks.add(rpm);
            return peaks;
        }

        boolean better(Point a, Point b) {
            boolean am = a.power >= target, bm = b.power >= target;
            if (am != bm) return am;
            if (!am) return a.power > b.power;
            return a.design.flow() < b.design.flow() || a.design.flow() == b.design.flow() && blocks(a.design) < blocks(b.design);
        }

        private double power(TurbineDesign design) {
            benches++;
            return Bench.turbine(design, data, steam, cancelled).output();
        }

        private double energyPerMilliBucket() {
            return steam.latentHeat * steam.turbineMultiplier;
        }

        /** Coil torque per RPM: the controller weighs each coil by its distance to the shaft. */
        private double torquePerRpm(TurbineDesign d) {
            return d.coilFactor() * data.extractionRate * Config.CONFIG.Turbine.CoilDragMultiplier;
        }

        /** Flow that holds the rotor at {@code rpm}: coil torque plus drag, at full blade efficiency. */
        private double flowAt(TurbineDesign d, double rpm) {
            var c = Config.CONFIG.Turbine;
            double metres = d.bladeLayers() * 4.0 * d.arm() * (d.arm() + 1) / 2;
            double mass = d.blades() * c.RotorAxialMassPerBlade + d.shaft() * c.RotorAxialMassPerShaft;
            double drag = mass * Math.pow(rpm * c.FrictionDragMultiplier, 2) + metres * Math.pow(rpm * c.AerodynamicDragMultiplier, 2);
            return (torquePerRpm(d) * rpm + drag) / energyPerMilliBucket();
        }

        List<Point> forArm(int arm, double peak) {
            double bonus = Math.max(1, data.bonus);
            // Torque needed at the peak for the target: P = (torque)^bonus * efficiency.
            double torque = Math.pow(target / Math.max(1e-6, data.efficiency), 1 / bonus);
            var empty = new TurbineDesign(arm, 1, 0, 0, coil, 0);
            int perLayer = empty.side() * empty.side() - 1;
            int coils = empty.coilsForFactor(torque / (peak * data.extractionRate * Config.CONFIG.Turbine.CoilDragMultiplier), maxShaft * perLayer);
            var probe = new TurbineDesign(arm, 1, 0, coils, coil, 0);
            double flow = flowAt(probe, peak);
            int blades = (int) Math.max(1, Math.ceil(flow / probe.bladeCapacity(peak)));
            var points = new ArrayList<Point>();
            // One blade layer more may let the rotor run more efficiently; both are kept, the size decides.
            for (int b = blades; b <= blades + 1; b++)
                for (int n = Math.max(1, coils - 1); n <= coils + 2; n++) {
                    var point = forCoils(arm, b, n, peak);
                    if (point != null) points.add(point);
                }
            return points;
        }

        /** Smallest whole flow reaching the target with power rising between the valley below the peak and just above it. */
        @Nullable
        private Point forCoils(int arm, int blades, int coils, double peak) {
            var design = new TurbineDesign(arm, blades, 0, coils, coil, 0);
            int spacer = Math.max(0, minShaft - design.shaft());
            design = new TurbineDesign(arm, blades, spacer, coils, coil, 0);
            if (design.shaft() > maxShaft) return null;
            long limit = design.flowLimit();
            long lo = (long) Math.floor(flowAt(design, peak / Math.sqrt(2))), hi = (long) Math.ceil(flowAt(design, peak * 1.066));
            lo = Math.max(1, Math.min(lo, limit));
            hi = Math.max(lo, Math.min(hi, limit));
            double top = power(design.withFlow(hi));
            if (top < target) return new Point(design.withFlow(hi), top);
            double bottom = power(design.withFlow(lo));
            if (bottom >= target) return new Point(design.withFlow(lo), bottom);
            double found = top;
            while (hi - lo > Math.max(1, hi / 1000)) {
                long mid = (lo + hi) / 2;
                double p = power(design.withFlow(mid));
                if (p >= target) {
                    hi = mid;
                    found = p;
                } else lo = mid;
            }
            return new Point(design.withFlow(hi), found);
        }
    }
}
