package biggerreactorsrework.compat.opencomputers.access;


import biggerreactorsrework.core.util.ThrowingFunctions;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.reactor.ReactorMultiblockController;
import biggerreactorsrework.multiblocks.reactor.simulation.IReactorSimulation;
import biggerreactorsrework.multiblocks.reactor.state.ReactorActivity;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

// Modified 2026-10-30: internal access facade for native server-thread callbacks.
public class ReactorAccess {
    
    public static ReactorAccess create(@Nonnull Supplier<ReactorMultiblockController> controllerSupplier) {
        return new ReactorAccess(controllerSupplier);
    }
    
    @Nonnull
    private final Supplier<ReactorMultiblockController> rawControllerSupplier;
    @Nonnull
    private final ThrowingFunctions.ThrowingSupplier<ReactorMultiblockController, IllegalArgumentException> controllerSupplier;
    @Nonnull
    private final ThrowingFunctions.ThrowingSupplier<IReactorSimulation, IllegalArgumentException> simulationSupplier;
    
    private ReactorAccess(@Nonnull Supplier<ReactorMultiblockController> rawControllerSupplier) {
        this.rawControllerSupplier = rawControllerSupplier;
        this.controllerSupplier = this::getController;
        this.simulationSupplier = this::getSimulation;
        battery = new Battery(simulationSupplier);
        coolantTank = new CoolantTank(controllerSupplier, simulationSupplier);
        fuelTank = new FuelTank(controllerSupplier, simulationSupplier);
    }
    
    public final String apiVersion() {
        return BiggerReactors.modVersion();
    }
    
    public final boolean connected() {
        ReactorMultiblockController controller = rawControllerSupplier.get();
        if (controller == null) {
            return false;
        }
        if (controller.simulation() == null) {
            return false;
        }
        return controller.assemblyState() == IValidatedMultiblock.AssemblyState.ASSEMBLED;
    }
    
    @Nonnull
    private ReactorMultiblockController getController() throws IllegalArgumentException {
        ReactorMultiblockController controller = rawControllerSupplier.get();
        if (controller == null || controller.assemblyState() != IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            throw new IllegalArgumentException("Invalid multiblock controller");
        }
        return controller;
    }
    
    @Nonnull
    private IReactorSimulation getSimulation() throws IllegalArgumentException {
        var sim = getController().simulation();
        if (sim == null) {

            throw new IllegalArgumentException("Invalid multiblock controller");
        }
        return sim;
    }
    
    public final boolean active() throws IllegalArgumentException {
        return controllerSupplier.get().isActive();
    }
    
    public final void setActive(boolean active) throws IllegalArgumentException {
        controllerSupplier.get().setActive(active ? ReactorActivity.ACTIVE : ReactorActivity.INACTIVE);
    }
    
    public static class Battery {
        
        @Nonnull
        private final ThrowingFunctions.ThrowingSupplier<IReactorSimulation.IBattery, IllegalArgumentException> batterySupplier;
        
        public Battery(@Nonnull ThrowingFunctions.ThrowingSupplier<IReactorSimulation, IllegalArgumentException> simulationSupplier) {
            this.batterySupplier = () -> {
                var sim = simulationSupplier.get();
                var battery = sim.battery();
                if (battery == null) {
                    throw new IllegalArgumentException("Invalid reactor state");
                }
                return battery;
            };
        }
        
        public final long stored() throws IllegalArgumentException {
            return batterySupplier.get().stored();
        }
        
        public final long capacity() throws IllegalArgumentException {
            return batterySupplier.get().capacity();
        }
        
        public final long producedLastTick() throws IllegalArgumentException {
            return batterySupplier.get().generatedLastTick();
        }
    }
    
    private final Battery battery;
    
    public final Battery battery() throws IllegalArgumentException {
        if (simulationSupplier.get().battery() == null) {
            return null;
        }
        return battery;
    }
    
    public static class CoolantTank {
        
        @Nonnull
        private final ThrowingFunctions.ThrowingSupplier<IReactorSimulation.ICoolantTank, IllegalArgumentException> coolantTankSupplier;
        @Nonnull
        private final ThrowingFunctions.ThrowingSupplier<ReactorTransitionTank, IllegalArgumentException> transitionTankSupplier;
        
        public CoolantTank(@Nonnull ThrowingFunctions.ThrowingSupplier<ReactorMultiblockController, IllegalArgumentException> controllerSupplier, @Nonnull ThrowingFunctions.ThrowingSupplier<IReactorSimulation, IllegalArgumentException> simulationSupplier) {
            this.coolantTankSupplier = () -> {
                var sim = simulationSupplier.get();
                var coolantTank = sim.coolantTank();
                if (coolantTank == null) {
                    throw new IllegalArgumentException("Invalid reactor state");
                }
                return coolantTank;
            };
            this.transitionTankSupplier = () -> {
                var controller = controllerSupplier.get();
                var transitionTank = controller.coolantTank();
                if (transitionTank == null) {
                    throw new IllegalArgumentException("Invalid reactor state");
                }
                return transitionTank;
            };
        }
        
        public final long coldFluidAmount() throws IllegalArgumentException {
            return coolantTankSupplier.get().liquidAmount();
        }
        
        public final long hotFluidAmount() throws IllegalArgumentException {
            return coolantTankSupplier.get().vaporAmount();
        }
        
        public final long capacity() throws IllegalArgumentException {
            return coolantTankSupplier.get().perSideCapacity();
        }














        
        public final long transitionedLastTick() throws IllegalArgumentException {
            return coolantTankSupplier.get().transitionedLastTick();
        }
        
        public final long maxTransitionedLastTick() throws IllegalArgumentException {
            return coolantTankSupplier.get().maxTransitionedLastTick();
        }
        
        public final void dump() throws IllegalArgumentException {
            transitionTankSupplier.get().dumpLiquid();
            transitionTankSupplier.get().dumpVapor();
        }
    }
    
    private final CoolantTank coolantTank;
    
    public final CoolantTank coolantTank() throws IllegalArgumentException {
        if (simulationSupplier.get().battery() != null) {
            return null;
        }
        return coolantTank;
    }
    
    public static class FuelTank {
        
        @Nonnull
        private final ThrowingFunctions.ThrowingSupplier<ReactorMultiblockController, IllegalArgumentException> controllerSupplier;
        @Nonnull
        private final ThrowingFunctions.ThrowingSupplier<IReactorSimulation, IllegalArgumentException> simulationSupplier;
        
        public FuelTank(@Nonnull ThrowingFunctions.ThrowingSupplier<ReactorMultiblockController, IllegalArgumentException> controllerSupplier, @Nonnull ThrowingFunctions.ThrowingSupplier<IReactorSimulation, IllegalArgumentException> simulationSupplier) {
            this.controllerSupplier = controllerSupplier;
            this.simulationSupplier = simulationSupplier;
        }
        
        public final long capacity() throws IllegalArgumentException {
            return simulationSupplier.get().fuelTank().capacity();
        }
        
        public final long totalReactant() throws IllegalArgumentException {
            return simulationSupplier.get().fuelTank().totalStored();
        }
        
        public final long fuel() throws IllegalArgumentException {
            return simulationSupplier.get().fuelTank().fuel();
        }
        
        public final long waste() throws IllegalArgumentException {
            return simulationSupplier.get().fuelTank().waste();
        }







        
        public final void ejectWaste() throws IllegalArgumentException {
            controllerSupplier.get().ejectWaste();
        }
        
        public final double fuelReactivity() throws IllegalArgumentException {
            return simulationSupplier.get().fertility();
        }
        
        public final double burnedLastTick() throws IllegalArgumentException {
            return simulationSupplier.get().fuelTank().burnedLastTick();
        }
    }
    
    private final FuelTank fuelTank;
    
    public final FuelTank fuelTank() throws IllegalArgumentException {
        return fuelTank;
    }
    
    public static class ControlRod {
        private final ThrowingFunctions.ThrowingSupplier<ReactorMultiblockController, IllegalArgumentException> controllerSupplier;
        private final int index;
        private boolean isValid = true;
        
        public ControlRod(ThrowingFunctions.ThrowingSupplier<ReactorMultiblockController, IllegalArgumentException> controllerSupplier, int index) {
            this.controllerSupplier = controllerSupplier;
            this.index = index;
        }
        
        public final boolean valid() {
            return isValid;
        }
        
        public final int index() {
            return index;
        }
        
        public final double level() throws IllegalArgumentException {
            if (!isValid) {
                throw new IllegalArgumentException("Invalid control rod object");
            }
            return controllerSupplier.get().controlRodLevel(index);
        }
        
        public final void setLevel(double newLevel) throws IllegalArgumentException {
            if (!isValid) {
                throw new IllegalArgumentException("Invalid control rod object");
            }
            controllerSupplier.get().setControlRodLevel(index, newLevel);
        }
        
        public final String name() throws IllegalArgumentException {
            if (!isValid) {
                throw new IllegalArgumentException("Invalid control rod object");
            }
            return controllerSupplier.get().controlRodName(index);
        }
        
        public final void setName(String newName) throws IllegalArgumentException {
            if (!isValid) {
                throw new IllegalArgumentException("Invalid control rod object");
            }
            if (newName.length() > 128) throw new IllegalArgumentException("Control rod name exceeds 128 characters");
            controllerSupplier.get().setControlRodName(index, newName);
        }
        
        void invalidate() {
            isValid = false;
        }
    }
    
    public final int controlRodCount() { return getController().controlRodCount(); }
    public final ControlRod getControlRod(int index) {
        if (index < 0 || index >= controlRodCount()) throw new IllegalArgumentException("Control rod index out of bounds");
        return new ControlRod(controllerSupplier, index);
    }
    public final void setAllControlRodLevels(double level) { getController().setAllControlRodLevels(level); }
    public final double fuelTemperature() throws IllegalArgumentException {
        return simulationSupplier.get().fuelHeat();
    }
    
    @Deprecated(forRemoval = true, since = "0.6.0")
    public double casingTemperature() throws IllegalArgumentException {
        return stackTemperature();
    }
    
    public final double stackTemperature() throws IllegalArgumentException {
        return simulationSupplier.get().stackHeat();
    }
    
    public final double ambientTemperature() throws IllegalArgumentException {
        return simulationSupplier.get().ambientTemperature();
    }
    
}
