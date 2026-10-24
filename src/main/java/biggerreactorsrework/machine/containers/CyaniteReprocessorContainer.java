package biggerreactorsrework.machine.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.machine.tiles.CyaniteReprocessorTile;
import biggerreactorsrework.machine.tiles.impl.CyaniteReprocessorItemHandler;
import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.registry.ContainerSupplier;
import biggerreactorsrework.core.registry.RegisterContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterContainer(name = "cyanite_reprocessor")
public class CyaniteReprocessorContainer extends AbstractContainerMenu implements GuiSync.IGUIPacketProvider {

    @RegisterContainer.Type
    public static MenuType<CyaniteReprocessorContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = CyaniteReprocessorContainer::new;

    private static final int MACHINE_SLOTS = CyaniteReprocessorItemHandler.SLOTS;

    private final Player player;
    private final CyaniteReprocessorTile tileEntity;

    public CyaniteReprocessorContainer(int windowId, BlockPos blockPos, Player player) {
        super(CyaniteReprocessorContainer.INSTANCE, windowId);
        this.player = player;
        this.tileEntity = (CyaniteReprocessorTile) player.level().getBlockEntity(blockPos);
        this.getGuiPacket();

        // Populate machine slots.
        if (this.tileEntity != null) {
            IItemHandler handler = tileEntity.getItemHandler();
            // Add input slot.
            this.addSlot(new SlotItemHandler(handler, CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX, 44, 41));
            // Add output slot.
            this.addSlot(new SlotItemHandler(handler, CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX, 116, 41));
        }

        // Populate player inventory.
        this.populatePlayerInventory();
    }

    /**
     * @return The current state of the machine.
     */
    @Nullable
    @Override
    public GuiSync.IGUIPacket getGuiPacket() {
        return this.tileEntity.cyaniteReprocessorState;
    }

    @Override
    public void executeRequest(String requestName, @Nullable Object requestData) {
        assert this.tileEntity.getLevel() != null;
        if (this.tileEntity.getLevel().isClientSide) {
            runRequest(requestName, requestData);
            return;
        }
        this.tileEntity.runRequest(requestName, requestData);
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        assert this.tileEntity.getLevel() != null;
        return stillValid(ContainerLevelAccess.create(tileEntity.getLevel(), tileEntity.getBlockPos()),
                player, CyaniteReprocessor.INSTANCE);
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemStackA = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack itemStackB = slot.getItem();
            itemStackA = itemStackB.copy();

            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(itemStackB, MACHINE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStackB, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStackB.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStackA;
    }

    /**
     * Draw and initialize the player's inventory.
     */
    private void populatePlayerInventory() {
        int guiOffset = 93;

        // Add player inventory;
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            for (int columnIndex = 0; columnIndex < 9; columnIndex++) {
                this.addSlot(new Slot(player.getInventory(), (columnIndex + rowIndex * 9 + 9),
                        (8 + columnIndex * 18), (guiOffset + rowIndex * 18)));
            }
        }
        // Add player hotbar.
        for (int columnIndex = 0; columnIndex < 9; columnIndex++) {
            this.addSlot(
                    new Slot(player.getInventory(), columnIndex, (8 + columnIndex * 18), (guiOffset + 58)));
        }
    }
}
