package biggerreactorsrework.core.transfer;

import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;

/** Single tank holding one fluid. Saved as the fluid stack itself, or {@code Empty} when there is none. */
public class SimpleTank implements FluidTanks {
    private FluidStack fluid = FluidStack.EMPTY;
    private final int capacity;
    private final Predicate<FluidStack> validator;

    public SimpleTank(int capacity) {
        this(capacity, stack -> true);
    }

    public SimpleTank(int capacity, Predicate<FluidStack> validator) {
        this.capacity = capacity;
        this.validator = validator;
    }

    protected void onContentsChanged() {
    }

    public FluidStack getFluid() {
        return fluid;
    }

    public void setFluid(FluidStack stack) {
        fluid = stack;
    }

    public int getFluidAmount() {
        return fluid.getAmount();
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSpace() {
        return Math.max(0, capacity - fluid.getAmount());
    }

    public boolean isEmpty() {
        return fluid.isEmpty();
    }

    public boolean isFluidValid(FluidStack stack) {
        return validator.test(stack);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return fluid;
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(resource)) {
            return 0;
        }
        if (!fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid, resource)) {
            return 0;
        }
        int filled = Math.min(getSpace(), resource.getAmount());
        if (filled > 0 && action.execute()) {
            if (fluid.isEmpty()) {
                fluid = resource.copyWithAmount(filled);
            } else {
                fluid.grow(filled);
            }
            onContentsChanged();
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(fluid, resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int drained = Math.min(fluid.getAmount(), maxDrain);
        if (drained <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack stack = fluid.copyWithAmount(drained);
        if (action.execute()) {
            fluid.shrink(drained);
            if (fluid.isEmpty()) {
                fluid = FluidStack.EMPTY;
            }
            onContentsChanged();
        }
        return stack;
    }

    public void readFromNBT(HolderLookup.Provider registries, CompoundTag tag) {
        fluid = TagStacks.parseFluid(registries, tag);
    }

    public CompoundTag writeToNBT(HolderLookup.Provider registries, CompoundTag tag) {
        if (!fluid.isEmpty()) {
            tag.merge(TagStacks.save(registries, fluid));
        }
        return tag;
    }
}
