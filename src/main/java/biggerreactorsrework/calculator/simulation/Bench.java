package biggerreactorsrework.calculator.simulation;

import biggerreactorsrework.Config;
import biggerreactorsrework.calculator.design.ExchangerDesign;
import biggerreactorsrework.calculator.design.ReactorDesign;
import biggerreactorsrework.calculator.design.TurbineDesign;
import biggerreactorsrework.multiblocks.reactor.simulation.SimulationConfiguration;
import biggerreactorsrework.multiblocks.reactor.simulation.SimulationDescription;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.core.component.DataComponentPatch;

import javax.annotation.Nullable;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Single machines with ideal supply: inlets refilled and outlets emptied every tick. Only for choosing candidates;
 * a plant is judged by {@link PlantRun} with finite charges and real transfers.
 */
public final class Bench {
    private Bench() { }

    public record Point(double output, double fuel, double rpm, boolean converged) { }

    private static final int STABLE = 3;

    /** Turbine fed exactly its nominal flow: gross FE/t exported and the speed it settles at. */
    public static Point turbine(TurbineDesign design, TurbineCoilRegistry.CoilData coil, FluidTransitionRegistry.FluidTransition steam, BooleanSupplier cancelled) {
        var sim = design.simulation(coil);
        int window = 400;
        double[] power = new double[STABLE], rpm = new double[STABLE];
        for (int w = 0; w < 600; w++) {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            double p = 0;
            for (int t = 0; t < window; t++) {
                sim.idealSupply(steam);
                sim.tick();
                p += sim.battery().extract(sim.battery().stored());
            }
            power[w % STABLE] = p / window;
            rpm[w % STABLE] = sim.RPM();
            if (w >= STABLE && agree(power, 5e-4) && agree(rpm, 5e-4)) return new Point(p / window, 0, sim.RPM(), true);
        }
        return new Point(power[0], 0, sim.RPM(), false);
    }

    /** Reactor at its insertion: FE/t for a solid reactor, hot output mB/t for a liquid one, and fuel mB/t. */
    public static Point reactor(ReactorDesign design, @Nullable ReactorModeratorRegistry.ModeratorProperties moderator,
                                @Nullable FluidTransitionRegistry.FluidTransition coolant, long maxTicks, BooleanSupplier cancelled) {
        SimulationDescription description = design.description(moderator);
        var sim = new SimulationDescription.Builder(Config.CONFIG.Reactor.useFullPassSimulation, false, false)
                .build(description, new SimulationConfiguration(Config.CONFIG.Reactor, VirtualPlant.AMBIENT, !design.liquid()));
        sim.setAllControlRodInsertions(design.insertion());
        var tank = sim.coolantTank() == null ? null : new ReactorTransitionTank(sim.coolantTank());
        int cycle = Math.max(1, design.columns() * design.y());
        int window = Math.max(cycle, (2000 + cycle - 1) / cycle * cycle);
        double[] output = new double[STABLE], fuel = new double[STABLE];
        long ticks = 0;
        int w = 0;
        while (ticks < maxTicks) {
            double o = 0, f = 0;
            for (int t = 0; t < window; t++, ticks++) {
                if ((ticks & 1023) == 0 && cancelled.getAsBoolean()) throw new CancellationException();
                sim.fuelTank().extractWaste(Long.MAX_VALUE, false);
                sim.fuelTank().insertFuel(Long.MAX_VALUE, false);
                if (tank != null && coolant != null) {
                    tank.fill(coolant.liquids.getFirst(), DataComponentPatch.EMPTY, tank.perSideCapacity() - tank.liquidAmount(), false);
                    tank.drain(tank.vaporType(), tank.fluidComponentsInTank(1), tank.vaporAmount(), false);
                }
                sim.tick(true);
                f += sim.fuelTank().burnedLastTick();
                o += tank != null ? sim.coolantTank().transitionedLastTick() : sim.battery().extract(sim.battery().stored());
            }
            output[w % STABLE] = o / window;
            fuel[w % STABLE] = f / window;
            w++;
            if (w >= STABLE && agree(output, .005) && agree(fuel, .005)) return new Point(o / window, f / window, 0, true);
        }
        return new Point(output[(w - 1 + STABLE) % STABLE], fuel[(w - 1 + STABLE) % STABLE], 0, false);
    }

    /** Exchanger with full hot inlet and full water inlet: steam mB/t (output) and hot coolant converted mB/t (fuel slot). */
    public static Point exchanger(ExchangerDesign design, FluidTransitionRegistry.FluidTransition coolant, FluidTransitionRegistry.FluidTransition water,
                                  BooleanSupplier cancelled) {
        var x = new VirtualPlant.Exchanger(design.geometry());
        var hot = coolant.gases.getFirst();
        var hotComponents = coolant.genericHot() ? biggerreactorsrework.registries.CoolantComponents.heated(coolant) : DataComponentPatch.EMPTY;
        int window = 1000;
        double[] steam = new double[STABLE], converted = new double[STABLE];
        for (int w = 0; w < 300; w++) {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            double s = 0, c = 0;
            for (int t = 0; t < window; t++) {
                x.primary.fill(hot, hotComponents, x.primary.perSideCapacity - x.primary.fluidAmountInTank(0), false);
                x.secondary.fill(water.liquids.getFirst(), DataComponentPatch.EMPTY, x.secondary.perSideCapacity - x.secondary.fluidAmountInTank(0), false);
                x.tick();
                s += x.secondary.transitionedLastTick();
                c += x.primary.transitionedLastTick();
                x.primary.drain(x.primary.fluidTypeInTank(1), x.primary.fluidComponentsInTank(1), x.primary.fluidAmountInTank(1), false);
                x.secondary.drain(x.secondary.fluidTypeInTank(1), x.secondary.fluidComponentsInTank(1), x.secondary.fluidAmountInTank(1), false);
            }
            steam[w % STABLE] = s / window;
            converted[w % STABLE] = c / window;
            if (w >= STABLE && agree(steam, 1e-3) && agree(converted, 1e-3)) return new Point(s / window, c / window, 0, true);
        }
        return new Point(steam[0], converted[0], 0, false);
    }

    private static boolean agree(double[] values, double tolerance) {
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (double v : values) {
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
        }
        return hi - lo <= tolerance * Math.max(Math.abs(hi), 1e-9);
    }
}
