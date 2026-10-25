package biggerreactorsrework.multiblocks.battery;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The battery's item slots: the first drains an item into the cells, the second charges an item from them. Both count
 * against the same per-tick limits as the ports.
 */
public class BatteryItemHandler extends ItemStackHandler {
    public static final int DISCHARGE_SLOT = 0;
    public static final int CHARGE_SLOT = 1;
    public static final int SLOTS = 2;
    
    private final Runnable onChanged;
    
    public BatteryItemHandler(Runnable onChanged) {
        super(SLOTS);
        this.onChanged = onChanged;
    }
    
    @Nullable
    public static IEnergyStorage energy(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }
    
    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        IEnergyStorage storage = energy(stack);
        if (storage == null) {
            return false;
        }
        return slot == DISCHARGE_SLOT ? storage.canExtract() : storage.canReceive();
    }
    
    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }
    
    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }
}
