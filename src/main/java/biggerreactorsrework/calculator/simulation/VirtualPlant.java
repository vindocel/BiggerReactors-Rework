package biggerreactorsrework.calculator.simulation;

import biggerreactorsrework.Config;
import biggerreactorsrework.core.fluids.IFluidTankHandler;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerBackpressure;
import biggerreactorsrework.multiblocks.reactor.simulation.IReactorSimulation;
import biggerreactorsrework.multiblocks.reactor.simulation.SimulationConfiguration;
import biggerreactorsrework.multiblocks.reactor.simulation.SimulationDescription;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
import biggerreactorsrework.multiblocks.turbine.simulation.ITurbineFluidTank;
import biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;

/**
 * The machines of a plant without a level: the reactor simulation, tanks and turbine simulation the controllers
 * use, ticked in controller order with the ports' push transfers between them. The exchanger's thermal step mirrors
 * {@code HeatExchangerMultiblockController.tick}. Fuel is topped up and waste removed before every reactor tick;
 * every FE the buffers hold is exported after its machine ticks; fluid charges are poured in until complete.
 */
public final class VirtualPlant {
    /** Overworld ambient temperature, the one every controller uses outside ultra-warm dimensions. */
    public static final double AMBIENT = 293.15;

    private final PlantSpec spec;
    final IReactorSimulation reactor;
    @Nullable final ReactorTransitionTank reactorTank;
    @Nullable final Exchanger exchanger;
    @Nullable final ModernTurbineSimulation turbine;
    private final boolean reverse;
    private long primaryLeft, waterLeft;

    public long exportedLastTick;
    public double burnedLastTick;

    public VirtualPlant(PlantSpec spec, boolean reverseOrder) {
        this.spec = spec;
        this.reverse = reverseOrder;
        SimulationDescription description = spec.reactor().description(spec.moderator());
        var configuration = new SimulationConfiguration(Config.CONFIG.Reactor, AMBIENT, spec.solid());
        reactor = new SimulationDescription.Builder(Config.CONFIG.Reactor.useFullPassSimulation, false, false).build(description, configuration);
        reactor.setAllControlRodInsertions(spec.reactor().insertion());
        reactorTank = reactor.coolantTank() == null ? null : new ReactorTransitionTank(reactor.coolantTank());
        exchanger = spec.exchanger() == null ? null : new Exchanger(spec.exchanger().geometry());
        turbine = spec.turbine() == null ? null : spec.turbine().simulation(spec.coil());
        primaryLeft = spec.solid() ? 0 : spec.primaryCharge();
        waterLeft = spec.advanced() ? spec.waterCharge() : 0;
    }

    public PlantSpec spec() { return spec; }

    public void tick() {
        pour();
        exportedLastTick = 0;
        if (reverse) {
            if (turbine != null) turbineStep();
            if (exchanger != null) exchangerStep();
            reactorStep();
        } else {
            reactorStep();
            if (exchanger != null) exchangerStep();
            if (turbine != null) turbineStep();
        }
    }

    /** The player pours the charges into the cold inlets as space frees up. */
    private void pour() {
        if (primaryLeft > 0 && reactorTank != null && spec.coolant() != null)
            primaryLeft -= reactorTank.fill(spec.coolant().liquids.getFirst(), DataComponentPatch.EMPTY, primaryLeft, false);
        if (waterLeft > 0 && exchanger != null && spec.water() != null)
            waterLeft -= exchanger.secondary.fill(spec.water().liquids.getFirst(), DataComponentPatch.EMPTY, waterLeft, false);
    }

    public boolean charged() { return primaryLeft == 0 && waterLeft == 0; }

    private void reactorStep() {
        reactor.fuelTank().extractWaste(Long.MAX_VALUE, false);
        reactor.fuelTank().insertFuel(Long.MAX_VALUE, false);
        reactor.tick(true);
        burnedLastTick = reactor.fuelTank().burnedLastTick();
        if (reactorTank != null) {
            reactorTank.sampleFlows();
            var fluid = reactorTank.vaporType();
            var components = reactorTank.fluidComponentsInTank(1);
            long amount = reactorTank.drain(fluid, components, reactorTank.vaporAmount(), true);
            if (exchanger != null) amount = exchanger.primary.fill(fluid, components, amount, false);
            else if (turbine != null) amount = turbine.fluidTank().fill(fluid, DataComponentPatch.EMPTY, amount, false);
            else amount = 0;
            reactorTank.drain(fluid, components, amount, false);
        } else if (reactor.battery() != null) {
            exportedLastTick += reactor.battery().extract(reactor.battery().stored());
        }
    }

    private void exchangerStep() {
        var x = exchanger;
        x.tick();
        push(x.primary, reactorTank, true);
        if (turbine != null) push(x.secondary, turbine.fluidTank(), false);
    }

    private static void push(FluidTransitionTank from, @Nullable IFluidTankHandler to, boolean keepComponents) {
        if (to == null) return;
        Fluid fluid = from.fluidTypeInTank(1);
        var components = from.fluidComponentsInTank(1);
        long amount = from.drain(fluid, components, from.fluidAmountInTank(1), true);
        amount = to.fill(fluid, keepComponents ? components : DataComponentPatch.EMPTY, amount, false);
        from.drain(fluid, components, amount, false);
    }

    private void turbineStep() {
        turbine.tick();
        exportedLastTick += turbine.battery().extract(turbine.battery().stored());
        ITurbineFluidTank tank = turbine.fluidTank();
        IFluidTankHandler target = exchanger != null ? exchanger.secondary : reactorTank;
        if (target == null) return;
        Fluid fluid = tank.liquidType();
        long amount = tank.drain(fluid, DataComponentPatch.EMPTY, tank.liquidAmount(), true);
        amount = target.fill(fluid, DataComponentPatch.EMPTY, amount, false);
        tank.drain(fluid, DataComponentPatch.EMPTY, amount, false);
    }

    /** Snapshot of every tank, in mB. */
    public record Stocks(long reactorCold, long reactorHot, long exchangerHot, long exchangerCold, long exchangerWater, long exchangerSteam,
                         long turbineSteam, long turbineWater) {
        public long primary(boolean advanced) {
            return advanced ? reactorCold + reactorHot + exchangerHot + exchangerCold : reactorCold + reactorHot + turbineSteam + turbineWater;
        }
        public long secondary() { return exchangerWater + exchangerSteam + turbineSteam + turbineWater; }
    }

    public Stocks stocks() {
        return new Stocks(reactorTank == null ? 0 : reactorTank.liquidAmount(), reactorTank == null ? 0 : reactorTank.vaporAmount(),
                exchanger == null ? 0 : exchanger.primary.fluidAmountInTank(0), exchanger == null ? 0 : exchanger.primary.fluidAmountInTank(1),
                exchanger == null ? 0 : exchanger.secondary.fluidAmountInTank(0), exchanger == null ? 0 : exchanger.secondary.fluidAmountInTank(1),
                turbine == null ? 0 : turbine.fluidTank().vaporAmount(), turbine == null ? 0 : turbine.fluidTank().liquidAmount());
    }

    /** Coolant or water the plant should hold right now: everything poured so far. */
    public long primaryPoured() { return spec.primaryCharge() - primaryLeft; }
    public long waterPoured() { return spec.waterCharge() - waterLeft; }

    public long reactorConversion() { return reactorTank == null ? 0 : reactor.coolantTank().transitionedLastTick(); }
    public long exchangerSteam() { return exchanger == null ? 0 : exchanger.secondary.transitionedLastTick(); }
    public long exchangerConversion() { return exchanger == null ? 0 : exchanger.primary.transitionedLastTick(); }
    public long turbineFlow() { return turbine == null ? 0 : turbine.flowLastTick(); }
    public double rpm() { return turbine == null ? 0 : turbine.RPM(); }
    public double fuelTemperature() { return reactor.fuelHeat(); }
    public double reactorOutputLimit() { return reactor.outputLimit(); }
    public double exchangerOutputLimit() { return exchanger == null ? 1 : exchanger.pressure.limit(); }

    /** Thermal step of the heat exchanger controller over the geometry of its assembled channels. */
    static final class Exchanger {
        final FluidTransitionTank primary = new FluidTransitionTank(true);
        final FluidTransitionTank secondary = new FluidTransitionTank(false);
        final HeatExchangerBackpressure pressure = new HeatExchangerBackpressure(primary, secondary);
        final HeatBody ambient = new HeatBody(), air = new HeatBody(), hot = new HeatBody(), cold = new HeatBody();
        private final biggerreactorsrework.calculator.design.ExchangerDesign.Geometry g;

        Exchanger(biggerreactorsrework.calculator.design.ExchangerDesign.Geometry g) {
            this.g = g;
            var cfg = Config.CONFIG.HeatExchanger;
            ambient.setTemperature(AMBIENT);
            air.setTemperature(AMBIENT);
            hot.setTemperature(AMBIENT);
            cold.setTemperature(AMBIENT);
            hot.setRfPerKelvin(g.condenser() * cfg.ChannelFEPerKelvinUnitVolume);
            cold.setRfPerKelvin(g.evaporator() * cfg.ChannelFEPerKelvinUnitVolume);
            primary.perSideCapacity = g.condenser() * cfg.ChannelTankVolumePerBlock;
            secondary.perSideCapacity = g.evaporator() * cfg.ChannelTankVolumePerBlock;
            air.setRfPerKelvin(g.airRFK());
            ambient.setInfinite(true);
        }

        void tick() {
            var cfg = Config.CONFIG.HeatExchanger;
            pressure.tick();
            primary.sampleFlows();
            secondary.sampleFlows();
            primary.transferWith(hot, g.condenser() * cfg.ChannelInternalSurfaceArea);
            hot.transferWith(air, g.condenserAirRFKT());
            hot.transferWith(cold, g.channelRFKT());
            cold.transferWith(air, g.evaporatorAirRFKT());
            secondary.transferWith(cold, g.evaporator() * cfg.ChannelInternalSurfaceArea);
            air.transferWith(ambient, g.ambientRFKT());
        }
    }
}
