package biggerreactorsrework.compat.computer.access;


import biggerreactorsrework.core.util.ThrowingFunctions;
import net.minecraft.core.registries.BuiltInRegistries;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.turbine.TurbineMultiblockController;
import biggerreactorsrework.multiblocks.turbine.state.VentState;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

// Modified 2026-10-30: internal access facade for native server-thread callbacks.
public class TurbineAccess {

    @Nonnull
    private final Supplier<TurbineMultiblockController> rawControllerSupplier;
    @Nonnull
    private final ThrowingFunctions.ThrowingSupplier<TurbineMultiblockController, IllegalArgumentException> controllerSupplier;

    public static TurbineAccess create(@Nonnull Supplier<TurbineMultiblockController> controllerSupplier) {
        return new TurbineAccess(controllerSupplier);
    }

    public TurbineAccess(@Nonnull Supplier<TurbineMultiblockController> rawControllerSupplier) {
        this.rawControllerSupplier = rawControllerSupplier;
        this.controllerSupplier = this::getController;
        battery = new Battery(controllerSupplier);
        rotor = new Rotor(controllerSupplier);
        tank = new FluidTank(controllerSupplier);
    }

    public final String apiVersion() {
        return BiggerReactors.modVersion();
    }

    public final boolean connected() {
        if (rawControllerSupplier.get() == null) {
            return false;
        }
        return rawControllerSupplier.get().assemblyState() == IValidatedMultiblock.AssemblyState.ASSEMBLED;
    }

    @Nonnull
    private TurbineMultiblockController getController() throws IllegalArgumentException {
        TurbineMultiblockController controller = rawControllerSupplier.get();
        if (controller == null || controller.assemblyState() != IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            throw new IllegalArgumentException("Invalid multiblock controller");
        }
        return controller;
    }

    public final boolean active() throws IllegalArgumentException {
        return controllerSupplier.get().simulation().active();
    }

    public final void setActive(boolean active) throws IllegalArgumentException {
        controllerSupplier.get().setActive(active);
    }


    public static class Battery {

        private final ThrowingFunctions.ThrowingSupplier<TurbineMultiblockController, IllegalArgumentException> controllerSupplier;

        public Battery(ThrowingFunctions.ThrowingSupplier<TurbineMultiblockController, IllegalArgumentException> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }

        public final long stored() throws IllegalArgumentException {
            return controllerSupplier.get().simulation().battery().stored();
        }

        public final long capacity() throws IllegalArgumentException {
            return controllerSupplier.get().simulation().battery().capacity();
        }

        public final long producedLastTick() throws IllegalArgumentException {
            return controllerSupplier.get().simulation().FEGeneratedLastTick();
        }
    }

    private final Battery battery;

    public final Battery battery() {
        return battery;
    }


    public static class Rotor {
        private final ThrowingFunctions.ThrowingSupplier<TurbineMultiblockController, IllegalArgumentException> controllerSupplier;

        public Rotor(ThrowingFunctions.ThrowingSupplier<TurbineMultiblockController, IllegalArgumentException> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }

        public final double RPM() throws IllegalArgumentException {
            return controllerSupplier.get().simulation().RPM();
        }

        public final double efficiencyLastTick() throws IllegalArgumentException {
            return controllerSupplier.get().simulation().bladeEfficiencyLastTick();
        }
    }

    private final Rotor rotor;

    public final Rotor rotor() {
        return rotor;
    }

    public static class FluidTank {
        private final ThrowingFunctions.ThrowingSupplier<TurbineMultiblockController, IllegalArgumentException> controllerSupplier;

        final TankFluid input;
        final TankFluid output;

        public FluidTank(ThrowingFunctions.ThrowingSupplier<TurbineMultiblockController, IllegalArgumentException> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
            input = new TankFluid(controllerSupplier, 0);
            output = new TankFluid(controllerSupplier, 1);
        }

        public static class TankFluid {
            final ThrowingFunctions.ThrowingSupplier<TurbineMultiblockController, IllegalArgumentException> controllerSupplier;
            final int tankNum;

            public TankFluid(ThrowingFunctions.ThrowingSupplier<TurbineMultiblockController, IllegalArgumentException> controllerSupplier, int tankNum) {
                this.controllerSupplier = controllerSupplier;
                this.tankNum = tankNum;
            }

            public final String name() throws IllegalArgumentException {
                return BuiltInRegistries.FLUID.getKey(controllerSupplier.get().simulation().fluidTank().fluidTypeInTank(tankNum)).toString();
            }

            public final long amount() throws IllegalArgumentException {
                return controllerSupplier.get().simulation().fluidTank().fluidAmountInTank(tankNum);
            }

            public final long maxAmount() throws IllegalArgumentException {
                return controllerSupplier.get().simulation().fluidTank().perSideCapacity();
            }
        }

        public final TankFluid input() {
            return input;
        }

        public final TankFluid output() {
            return output;
        }

        public final long flowLastTick() throws IllegalArgumentException {
            return controllerSupplier.get().simulation().flowLastTick();
        }

        public final long nominalFlowRate() throws IllegalArgumentException {
            return controllerSupplier.get().simulation().nominalFlowRate();
        }

        public final void setNominalFlowRate(long rate) throws IllegalArgumentException {
            controllerSupplier.get().simulation().setNominalFlowRate(rate);
        }


        public final long flowRateLimit() throws IllegalArgumentException {
            return controllerSupplier.get().simulation().flowRateLimit();
        }
    }

    private final FluidTank tank;

    public final FluidTank fluidTank() {
        return tank;
    }

    public final boolean coilEngaged() throws IllegalArgumentException {
        return controllerSupplier.get().simulation().coilEngaged();
    }

    public final void setCoilEngaged(boolean engaged) throws IllegalArgumentException {
        controllerSupplier.get().simulation().setCoilEngaged(engaged);
    }

    public String ventState() { return getController().simulation().ventState().name().toLowerCase(java.util.Locale.ROOT); }
    public void setVentState(String state) {
        VentState value = switch (state) {
            case "closed" -> VentState.CLOSED;
            case "overflow" -> VentState.OVERFLOW;
            case "all" -> VentState.ALL;
            default -> throw new IllegalArgumentException("Expected closed, overflow or all");
        };
        getController().simulation().setVentState(value);
    }
}
