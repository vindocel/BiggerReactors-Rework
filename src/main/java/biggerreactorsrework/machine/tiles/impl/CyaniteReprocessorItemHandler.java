package biggerreactorsrework.machine.tiles.impl;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import biggerreactorsrework.items.ingots.BlutoniumIngot;
import biggerreactorsrework.items.ingots.CyaniteIngot;

import javax.annotation.Nonnull;

public class CyaniteReprocessorItemHandler extends ItemStackHandler {
    
    public static final int INPUT_SLOT_INDEX = 0;
    public static final int OUTPUT_SLOT_INDEX = 1;
    public static final int SLOTS = 2;
    
    private final Runnable onChanged;
    private final IItemHandler pipeHandler = this.createPipeHandler(true, true);
    private final IItemHandler inputHandler = this.createPipeHandler(true, false);
    private final IItemHandler outputHandler = this.createPipeHandler(false, true);
    
    public CyaniteReprocessorItemHandler(Runnable onChanged) {
        super(SLOTS);
        this.onChanged = onChanged;
    }
    
    @Override
    protected void onContentsChanged(int slot) {
        this.onChanged.run();
    }
    
    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        switch (slot) {
            case INPUT_SLOT_INDEX:
                return stack.getItem() == CyaniteIngot.INSTANCE;
            case OUTPUT_SLOT_INDEX:
                return stack.getItem() == BlutoniumIngot.INSTANCE;
        }
        throw new IndexOutOfBoundsException();
    }
    
    /**
     * @return The view exposed to pipes and hoppers: insert into the input slot only, extract from the output slot only.
     */
    public IItemHandler pipeHandler() {
        return this.pipeHandler;
    }
    
    /**
     * @param insert Whether the view accepts cyanite into the input slot.
     * @param extract Whether the view gives blutonium from the output slot.
     * @return The view for a side with that item route, or null when it does neither.
     */
    public IItemHandler sideHandler(boolean insert, boolean extract) {
        if (insert && extract) {
            return this.pipeHandler;
        }
        return insert ? this.inputHandler : extract ? this.outputHandler : null;
    }
    
    private IItemHandler createPipeHandler(boolean insert, boolean extract) {
        final CyaniteReprocessorItemHandler realHandler = this;
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return realHandler.getSlots();
            }
            
            @Nonnull
            @Override
            public ItemStack getStackInSlot(int slot) {
                return realHandler.getStackInSlot(slot);
            }
            
            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                if (!insert || slot != INPUT_SLOT_INDEX) {
                    return stack;
                }
                return realHandler.insertItem(slot, stack, simulate);
            }
            
            @Nonnull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!extract || slot != OUTPUT_SLOT_INDEX) {
                    return ItemStack.EMPTY;
                }
                return realHandler.extractItem(slot, amount, simulate);
            }
            
            @Override
            public int getSlotLimit(int slot) {
                return realHandler.getSlotLimit(slot);
            }
            
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return realHandler.isItemValid(slot, stack);
            }
        };
    }
}
