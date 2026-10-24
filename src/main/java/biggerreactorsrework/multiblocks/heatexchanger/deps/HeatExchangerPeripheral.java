package biggerreactorsrework.multiblocks.heatexchanger.deps;

import java.util.Set;
import biggerreactorsrework.core.util.ThrowingFunctions;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.registries.BuiltInRegistries;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerMultiblockController;
import biggerreactorsrework.util.FluidTransitionTank;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.util.HeatBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class HeatExchangerPeripheral implements IPeripheral {

    final Supplier<HeatExchangerMultiblockController> rawControllerSupplier;
    final ThrowingFunctions.ThrowingSupplier<HeatExchangerMultiblockController, LuaException> controllerSupplier;
    final ThrowingFunctions.ThrowingSupplier<Lock, LuaException> lockSupplier;

    final Channel condenser;
    final Channel evaporator;
    final InternalEnvironment internalEnvironment;

    public static HeatExchangerPeripheral create(@Nonnull Supplier<HeatExchangerMultiblockController> controllerSupplier) {
        return new HeatExchangerPeripheral(controllerSupplier);
    }

    public HeatExchangerPeripheral(Supplier<HeatExchangerMultiblockController> rawControllerSupplier) {
        this.rawControllerSupplier = rawControllerSupplier;
        this.controllerSupplier = this::getController;
        lockSupplier = () -> controllerSupplier.get().locks.readLock();
        condenser = new Channel(() -> controllerSupplier.get().condenserHeatBody, () -> controllerSupplier.get().condenserTank, () -> controllerSupplier.get().condenserChannels.size(), lockSupplier);
        evaporator = new Channel(() -> controllerSupplier.get().evaporatorHeatBody, () -> controllerSupplier.get().evaporatorTank, () -> controllerSupplier.get().evaporatorChannels.size(), lockSupplier);
        internalEnvironment = new InternalEnvironment(() -> controllerSupplier.get().airHeatBody);
    }

    @LuaFunction
    public String apiVersion() {
        return BiggerReactors.modVersion();
    }

    @LuaFunction
    public boolean connected() {
        HeatExchangerMultiblockController controller = rawControllerSupplier.get();
        if (controller == null) {
            return false;
        }
        return controller.assemblyState() == IValidatedMultiblock.AssemblyState.ASSEMBLED;
    }

    @Nonnull
    private HeatExchangerMultiblockController getController() throws LuaException {
        HeatExchangerMultiblockController controller = rawControllerSupplier.get();
        if (controller == null || controller.assemblyState() != IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            throw new LuaException("Invalid multiblock controller");
        }
        return controller;
    }


    public static class Channel {
        final ThrowingFunctions.ThrowingSupplier<HeatBody, LuaException> heatBodySupplier;
        final ThrowingFunctions.ThrowingSupplier<FluidTransitionTank, LuaException> transitionTankSupplier;
        final ThrowingFunctions.ThrowingSupplier<Integer, LuaException> countSupplier;
        final ThrowingFunctions.ThrowingSupplier<Lock, LuaException> lockSupplier;

        final ChannelFluid inputFluid;
        final ChannelFluid outputFluid;

        Channel(ThrowingFunctions.ThrowingSupplier<HeatBody, LuaException> heatBodySupplier, ThrowingFunctions.ThrowingSupplier<FluidTransitionTank, LuaException> transitionTankSupplier,
                ThrowingFunctions.ThrowingSupplier<Integer, LuaException> countSupplier, ThrowingFunctions.ThrowingSupplier<Lock, LuaException> lockSupplier) {
            this.heatBodySupplier = heatBodySupplier;
            this.transitionTankSupplier = transitionTankSupplier;
            this.countSupplier = countSupplier;
            this.lockSupplier = lockSupplier;
            inputFluid = new ChannelFluid(transitionTankSupplier, 0);
            outputFluid = new ChannelFluid(transitionTankSupplier, 1);
        }

        public static class ChannelFluid {
            final ThrowingFunctions.ThrowingSupplier<FluidTransitionTank, LuaException> transitionTankSupplier;
            final int tankNum;

            public ChannelFluid(ThrowingFunctions.ThrowingSupplier<FluidTransitionTank, LuaException> transitionTankSupplier, int tankNum) {
                this.transitionTankSupplier = transitionTankSupplier;
                this.tankNum = tankNum;
            }

            @LuaFunction
            public String name() throws LuaException {
                return BuiltInRegistries.FLUID.getKey(transitionTankSupplier.get().fluidTypeInTank(tankNum)).toString();
            }

            @LuaFunction
            public long amount() throws LuaException {
                return transitionTankSupplier.get().fluidAmountInTank(tankNum);
            }

            @LuaFunction
            public long maxAmount() throws LuaException {
                return transitionTankSupplier.get().perSideCapacity;
            }
        }

        @LuaFunction
        public double temperature() throws LuaException {
            return heatBodySupplier.get().temperature();
        }

        @LuaFunction
        public double rfPerKelvin() throws LuaException {
            return heatBodySupplier.get().rfPerKelvin();
        }

        @LuaFunction
        public ChannelFluid input() {
            return inputFluid;
        }

        @LuaFunction
        public ChannelFluid output() {
            return outputFluid;
        }

        @LuaFunction
        public long transitionedLastTick() throws LuaException {
            return transitionTankSupplier.get().transitionedLastTick();
        }

        @LuaFunction
        public long maxTransitionedLastTick() throws LuaException {
            return transitionTankSupplier.get().maxTransitionedLastTick();
        }

        @LuaFunction
        public double transitionEnergy() throws LuaException {
            return transitionTankSupplier.get().activeTransition().latentHeat;
        }
        
        @LuaFunction
        public void dump() throws LuaException {
            transitionTankSupplier.get().dumpTank(0);
            transitionTankSupplier.get().dumpTank(1);
        }
    }

    @LuaFunction
    public Channel condenser() {
        return condenser;
    }

    @LuaFunction
    public Channel evaporator() {
        return evaporator;
    }

    public static class InternalEnvironment {
        final ThrowingFunctions.ThrowingSupplier<HeatBody, LuaException> heatBodySupplier;

        public InternalEnvironment(ThrowingFunctions.ThrowingSupplier<HeatBody, LuaException> heatBodySupplier) {
            this.heatBodySupplier = heatBodySupplier;
        }

        @LuaFunction
        public double temperature() throws LuaException {
            return heatBodySupplier.get().temperature();
        }

        @LuaFunction
        public double rfPerKelvin() throws LuaException {
            return heatBodySupplier.get().rfPerKelvin();
        }

    }

    @LuaFunction
    public InternalEnvironment internalEnvironment() {
        return internalEnvironment;
    }

    @LuaFunction
    public double ambientTemperature() throws LuaException {
        return controllerSupplier.get().ambientHeatBody.temperature();
    }

    @LuaFunction
    public double ambientInternalRFKT() throws LuaException {
        return controllerSupplier.get().airAmbientRFKT;
    }

    @LuaFunction
    public double condenserInternalRFKT() throws LuaException {
        return controllerSupplier.get().condenserAirRFKT;
    }

    @LuaFunction
    public double evaporatorInternalRFKT() throws LuaException {
        return controllerSupplier.get().evaporatorAirRFKT;
    }

    @LuaFunction
    public double condenserEvaporatorRFKT() throws LuaException {
        return controllerSupplier.get().channelRFKT;
    }

    @Nonnull
    @Override
    public String getType() {
        // legacy type kept as the primary one so existing scripts comparing peripheral.getType() keep working
        return "BiggerReactors_Heat-Exchanger";
    }
    
    @Override
    public Set<String> getAdditionalTypes() {
        return Set.of("biggerreactorsrework:heat_exchanger");
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if(other == this){
            return true;
        }
        if (other instanceof HeatExchangerPeripheral) {
            if (rawControllerSupplier.get() == null) {
                return false;
            }
            return ((HeatExchangerPeripheral) other).rawControllerSupplier.get() == rawControllerSupplier.get();
        }
        return false;
    }
}
