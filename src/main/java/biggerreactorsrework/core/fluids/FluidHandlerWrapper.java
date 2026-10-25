package biggerreactorsrework.core.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidHandlerWrapper implements IFluidTankHandler {

    public static IFluidTankHandler wrap(IFluidHandler handler) {
        if (handler instanceof IFluidTankHandler) {
            return (IFluidTankHandler) handler;
        }
        return new FluidHandlerWrapper(handler);
    }

    final IFluidHandler handler;

    private FluidHandlerWrapper(IFluidHandler handler) {
        this.handler = handler;
    }

    @Override
    public int tankCount() {
        return handler.getTanks();
    }

    @Override
    public long tankCapacity(int tank) {
        return handler.getTankCapacity(tank);
    }

    @Override
    public Fluid fluidTypeInTank(int tank) {
        return handler.getFluidInTank(tank).getFluid();
    }

    @Override
    public DataComponentPatch fluidComponentsInTank(int tank) {
        return handler.getFluidInTank(tank).getComponentsPatch();
    }

    @Override
    public long fluidAmountInTank(int tank) {
        return handler.getFluidInTank(tank).getAmount();
    }

    @Override
    public boolean fluidValidForTank(int tank, Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return false;
        }
        return handler.isFluidValid(tank, new FluidStack(fluid, 1));
    }

    private static FluidStack stackOf(Fluid fluid, DataComponentPatch components, long amount) {
        return new FluidStack(fluid.builtInRegistryHolder(), (int) Math.min(Integer.MAX_VALUE, amount), components);
    }

    @Override
    public long fill(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return 0;
        }
        long offered = Math.min(Integer.MAX_VALUE, amount);
        return biggerreactorsrework.core.energy.PowerDistribution.bounded(offered, handler.fill(stackOf(fluid, components, offered), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE));
    }

    @Override
    public long drain(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return 0;
        }
        FluidStack drained = handler.drain(stackOf(fluid, components, amount), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
        if (drained.isEmpty() || drained.getFluid() != fluid || !drained.getComponentsPatch().equals(components)) return 0;
        return biggerreactorsrework.core.energy.PowerDistribution.bounded(Math.min(Integer.MAX_VALUE, amount), drained.getAmount());
    }
}
