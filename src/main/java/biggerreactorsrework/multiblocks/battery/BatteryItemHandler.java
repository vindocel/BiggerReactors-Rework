package biggerreactorsrework.multiblocks.battery;

import biggerreactorsrework.multiblocks.battery.blocks.BatteryCell;
import net.minecraft.world.item.ItemStack;
//? if =26.1.2 || =26.3 {
import net.neoforged.neoforge.capabilities.Capabilities;
import biggerreactorsrework.core.transfer.EnergyStore;
import biggerreactorsrework.core.transfer.SlotStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The battery's item slots: the first drains an item into the cells, the second charges an item from them. Both count
 * against the same per-tick limits as the ports.
 */
public class BatteryItemHandler extends SlotStorage {
    public static final int DISCHARGE_SLOT = 0;
//?} else if =1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/^*
 * The battery's item slots: the first drains an item into the cells, the second charges an item from them. Both count
 * against the same per-tick limits as the ports.
 ^/
public class BatteryItemHandler extends ItemStackHandler {
    public static final int DISCHARGE_SLOT = 0;
*/
//?}
    public static final int CHARGE_SLOT = 1;
    public static final int SLOTS = 2;

    private final Runnable onChanged;

    public BatteryItemHandler(Runnable onChanged) {
        super(SLOTS);
        this.onChanged = onChanged;
    }

//? if =26.1.2 || =26.3 {
    @Nullable
    public static EnergyStore energy(ItemStack stack) {
        return stack.isEmpty() ? null : biggerreactorsrework.core.transfer.Transfer.energy(net.neoforged.neoforge.transfer.access.ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM));
    }
//?} else if =1.21.1 {
/*    @Nullable
    public static IEnergyStorage energy(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }
*/
//?}

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        if (stack.is(BatteryCell.INSTANCE.asItem())) {
            return slot == DISCHARGE_SLOT && BatteryCell.storedEnergy(stack) > 0;
//? if =26.1.2 || =26.3 {
        }
        EnergyStore storage = energy(stack);
        if (storage == null) {
//?} else if =1.21.1 {
/*        }
        IEnergyStorage storage = energy(stack);
        if (storage == null) {
*/
//?}
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
