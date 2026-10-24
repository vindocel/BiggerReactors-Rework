package biggerreactorsrework.core.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Long-capacity tank view exposed as a NeoForge {@link IFluidHandler}.
 * Amounts larger than {@link Integer#MAX_VALUE} are clamped only at the NeoForge boundary.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IFluidTankHandler extends IFluidHandler {

    default IFluidHandler fluidHandlerCapability() {
        return this;
    }

    @Override
    default int getTanks() {
        return tankCount();
    }

    @Nonnull
    @Override
    default FluidStack getFluidInTank(int tank) {
        final var fluid = fluidTypeInTank(tank);
        final long amount = fluidAmountInTank(tank);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid.builtInRegistryHolder(), clampToInt(amount), fluidComponentsInTank(tank));
    }

    @Override
    default int getTankCapacity(int tank) {
        return clampToInt(tankCapacity(tank));
    }

    @Override
    default boolean isFluidValid(int tank, FluidStack resource) {
        return fluidValidForTank(tank, resource.getFluid());
    }

    @Override
    default int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        return clampToInt(fill(resource.getFluid(), resource.getComponentsPatch(), resource.getAmount(), action.simulate()));
    }

    @Nonnull
    @Override
    default FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int drainedAmount = clampToInt(drain(resource.getFluid(), resource.getComponentsPatch(), resource.getAmount(), action.simulate()));
        if (drainedAmount <= 0) {
            return FluidStack.EMPTY;
        }
        return resource.copyWithAmount(drainedAmount);
    }

    @Nonnull
    @Override
    default FluidStack drain(int maxDrain, FluidAction action) {
        for (int i = 0; i < tankCount(); i++) {
            final var fluid = fluidTypeInTank(i);
            if (fluid == Fluids.EMPTY) {
                continue;
            }
            final var components = fluidComponentsInTank(i);
            int drainedAmount = clampToInt(drain(fluid, components, maxDrain, action.simulate()));
            if (drainedAmount > 0) {
                return new FluidStack(fluid.builtInRegistryHolder(), drainedAmount, components);
            }
        }
        return FluidStack.EMPTY;
    }

    private static int clampToInt(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }

    int tankCount();

    long tankCapacity(int tank);

    Fluid fluidTypeInTank(int tank);

    /**
     * Data components carried by the fluid in the tank; {@link DataComponentPatch#EMPTY} when none.
     */
    default DataComponentPatch fluidComponentsInTank(int tank) {
        return DataComponentPatch.EMPTY;
    }

    long fluidAmountInTank(int tank);

    boolean fluidValidForTank(int tank, Fluid fluid);

    long fill(Fluid fluid, DataComponentPatch components, long amount, boolean simulate);

    long drain(Fluid fluid, DataComponentPatch components, long amount, boolean simulate);
}
