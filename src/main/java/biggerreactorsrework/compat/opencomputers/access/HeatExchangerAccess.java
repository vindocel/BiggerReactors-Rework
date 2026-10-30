package biggerreactorsrework.compat.opencomputers.access;


import biggerreactorsrework.core.util.ThrowingFunctions;
import net.minecraft.core.registries.BuiltInRegistries;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerMultiblockController;
import biggerreactorsrework.util.FluidTransitionTank;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.util.HeatBody;

import javax.annotation.Nonnull;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

// Modified 2026-10-30: internal access facade for native server-thread callbacks.
public class HeatExchangerAccess {

    final Supplier<HeatExchangerMultiblockController> rawControllerSupplier;
    final ThrowingFunctions.ThrowingSupplier<HeatExchangerMultiblockController, IllegalArgumentException> controllerSupplier;
    final ThrowingFunctions.ThrowingSupplier<Lock, IllegalArgumentException> lockSupplier;

    final Channel condenser;
    final Channel evaporator;
    final InternalEnvironment internalEnvironment;

    public static HeatExchangerAccess create(@Nonnull Supplier<HeatExchangerMultiblockController> controllerSupplier) {
        return new HeatExchangerAccess(controllerSupplier);
    }

    public HeatExchangerAccess(Supplier<HeatExchangerMultiblockController> rawControllerSupplier) {
        this.rawControllerSupplier = rawControllerSupplier;
        this.controllerSupplier = this::getController;
        lockSupplier = () -> controllerSupplier.get().locks.readLock();
        condenser = new Channel(() -> controllerSupplier.get().condenserHeatBody, () -> controllerSupplier.get().condenserTank, () -> controllerSupplier.get().condenserChannels.size(), lockSupplier);
        evaporator = new Channel(() -> controllerSupplier.get().evaporatorHeatBody, () -> controllerSupplier.get().evaporatorTank, () -> controllerSupplier.get().evaporatorChannels.size(), lockSupplier);
        internalEnvironment = new InternalEnvironment(() -> controllerSupplier.get().airHeatBody);
    }

    public final String apiVersion() {
        return BiggerReactors.modVersion();
    }

    public final boolean connected() {
        HeatExchangerMultiblockController controller = rawControllerSupplier.get();
        if (controller == null) {
            return false;
        }
        return controller.assemblyState() == IValidatedMultiblock.AssemblyState.ASSEMBLED;
    }

    @Nonnull
    private HeatExchangerMultiblockController getController() throws IllegalArgumentException {
        HeatExchangerMultiblockController controller = rawControllerSupplier.get();
        if (controller == null || controller.assemblyState() != IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            throw new IllegalArgumentException("Invalid multiblock controller");
        }
        return controller;
    }


    public static class Channel {
        final ThrowingFunctions.ThrowingSupplier<HeatBody, IllegalArgumentException> heatBodySupplier;
        final ThrowingFunctions.ThrowingSupplier<FluidTransitionTank, IllegalArgumentException> transitionTankSupplier;
        final ThrowingFunctions.ThrowingSupplier<Integer, IllegalArgumentException> countSupplier;
        final ThrowingFunctions.ThrowingSupplier<Lock, IllegalArgumentException> lockSupplier;

        final ChannelFluid inputFluid;
        final ChannelFluid outputFluid;

        Channel(ThrowingFunctions.ThrowingSupplier<HeatBody, IllegalArgumentException> heatBodySupplier, ThrowingFunctions.ThrowingSupplier<FluidTransitionTank, IllegalArgumentException> transitionTankSupplier,
                ThrowingFunctions.ThrowingSupplier<Integer, IllegalArgumentException> countSupplier, ThrowingFunctions.ThrowingSupplier<Lock, IllegalArgumentException> lockSupplier) {
            this.heatBodySupplier = heatBodySupplier;
            this.transitionTankSupplier = transitionTankSupplier;
            this.countSupplier = countSupplier;
            this.lockSupplier = lockSupplier;
            inputFluid = new ChannelFluid(transitionTankSupplier, 0);
            outputFluid = new ChannelFluid(transitionTankSupplier, 1);
        }

        public static class ChannelFluid {
            final ThrowingFunctions.ThrowingSupplier<FluidTransitionTank, IllegalArgumentException> transitionTankSupplier;
            final int tankNum;

            public ChannelFluid(ThrowingFunctions.ThrowingSupplier<FluidTransitionTank, IllegalArgumentException> transitionTankSupplier, int tankNum) {
                this.transitionTankSupplier = transitionTankSupplier;
                this.tankNum = tankNum;
            }

            public final String name() throws IllegalArgumentException {
                return BuiltInRegistries.FLUID.getKey(transitionTankSupplier.get().fluidTypeInTank(tankNum)).toString();
            }

            public final long amount() throws IllegalArgumentException {
                return transitionTankSupplier.get().fluidAmountInTank(tankNum);
            }

            public final long maxAmount() throws IllegalArgumentException {
                return transitionTankSupplier.get().perSideCapacity;
            }
        }

        public final double temperature() throws IllegalArgumentException {
            return heatBodySupplier.get().temperature();
        }

        public final double rfPerKelvin() throws IllegalArgumentException {
            return heatBodySupplier.get().rfPerKelvin();
        }

        public final ChannelFluid input() {
            return inputFluid;
        }

        public final ChannelFluid output() {
            return outputFluid;
        }

        public final long transitionedLastTick() throws IllegalArgumentException {
            return transitionTankSupplier.get().transitionedLastTick();
        }

        public final long maxTransitionedLastTick() throws IllegalArgumentException {
            return transitionTankSupplier.get().maxTransitionedLastTick();
        }

        public final double transitionEnergy() throws IllegalArgumentException {
            var transition = transitionTankSupplier.get().activeTransition();
            return transition == null ? 0 : transition.latentHeat;
        }
        
        public final void dump() throws IllegalArgumentException {
            transitionTankSupplier.get().dumpTank(0);
            transitionTankSupplier.get().dumpTank(1);
        }
    }

    public final Channel condenser() {
        return condenser;
    }

    public final Channel evaporator() {
        return evaporator;
    }

    public static class InternalEnvironment {
        final ThrowingFunctions.ThrowingSupplier<HeatBody, IllegalArgumentException> heatBodySupplier;

        public InternalEnvironment(ThrowingFunctions.ThrowingSupplier<HeatBody, IllegalArgumentException> heatBodySupplier) {
            this.heatBodySupplier = heatBodySupplier;
        }

        public final double temperature() throws IllegalArgumentException {
            return heatBodySupplier.get().temperature();
        }

        public final double rfPerKelvin() throws IllegalArgumentException {
            return heatBodySupplier.get().rfPerKelvin();
        }

    }

    public final InternalEnvironment internalEnvironment() {
        return internalEnvironment;
    }

    public final double ambientTemperature() throws IllegalArgumentException {
        return controllerSupplier.get().ambientHeatBody.temperature();
    }

    public final double ambientInternalRFKT() throws IllegalArgumentException {
        return controllerSupplier.get().airAmbientRFKT;
    }

    public final double condenserInternalRFKT() throws IllegalArgumentException {
        return controllerSupplier.get().condenserAirRFKT;
    }

    public final double evaporatorInternalRFKT() throws IllegalArgumentException {
        return controllerSupplier.get().evaporatorAirRFKT;
    }

    public final double condenserEvaporatorRFKT() throws IllegalArgumentException {
        return controllerSupplier.get().channelRFKT;
    }

}
