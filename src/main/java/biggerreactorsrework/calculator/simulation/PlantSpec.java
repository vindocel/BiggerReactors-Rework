package biggerreactorsrework.calculator.simulation;

import biggerreactorsrework.calculator.design.ExchangerDesign;
import biggerreactorsrework.calculator.design.ReactorDesign;
import biggerreactorsrework.calculator.design.TurbineDesign;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;

import javax.annotation.Nullable;

/**
 * Everything a virtual plant needs: machine designs, resolved material properties and the finite fluid charges.
 * The primary charge is the reactor coolant (water in the direct route); the water charge is the exchanger's
 * secondary circuit. Both are poured through the inlet named in {@link VirtualPlant}.
 */
public record PlantSpec(ReactorDesign reactor, @Nullable ReactorModeratorRegistry.ModeratorProperties moderator,
                        @Nullable FluidTransitionRegistry.FluidTransition coolant, @Nullable ExchangerDesign exchanger,
                        @Nullable FluidTransitionRegistry.FluidTransition water, @Nullable TurbineDesign turbine,
                        @Nullable TurbineCoilRegistry.CoilData coil, long primaryCharge, long waterCharge) {
    public boolean solid() { return !reactor.liquid(); }
    public boolean advanced() { return exchanger != null; }

    public PlantSpec withReactor(ReactorDesign value) {
        return new PlantSpec(value, moderator, coolant, exchanger, water, turbine, coil, primaryCharge, waterCharge);
    }

    public PlantSpec withTurbine(TurbineDesign value) {
        return new PlantSpec(reactor, moderator, coolant, exchanger, water, value, coil, primaryCharge, waterCharge);
    }

    public PlantSpec withExchanger(ExchangerDesign value) {
        return new PlantSpec(reactor, moderator, coolant, value, water, turbine, coil, primaryCharge, waterCharge);
    }

    public PlantSpec withCharges(long primary, long secondary) {
        return new PlantSpec(reactor, moderator, coolant, exchanger, water, turbine, coil, primary, secondary);
    }

    /** Coolant volume the reactor holds per side, as {@code BaseReactorSimulation} sizes it. */
    public long reactorTankCapacity() {
        return reactor.manifoldBlocks() * biggerreactorsrework.Config.CONFIG.Reactor.CoolantTankAmountPerFuelRod;
    }
}
