package biggerreactorsrework.multiblocks.battery.containers;

import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.registry.ContainerSupplier;
import biggerreactorsrework.core.registry.RegisterContainer;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryRedstonePort;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryRedstonePortTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Added 2026-10-30: configuration stays bound to the clicked sensor and its menu. */
@RegisterContainer(name = "battery_redstone_port")
public final class BatteryRedstonePortContainer extends AbstractContainerMenu implements GuiSync.IGUIPacketProvider {
    @RegisterContainer.Type public static MenuType<BatteryRedstonePortContainer> INSTANCE;
    @RegisterContainer.Supplier public static final ContainerSupplier SUPPLIER = BatteryRedstonePortContainer::new;
    public final BatteryRedstonePortTile tile;
    public BatteryRedstonePortContainer(int id, BlockPos pos, Player player) {
        super(INSTANCE, id);
        tile = (BatteryRedstonePortTile) player.level().getBlockEntity(pos);
    }
    @Override public boolean stillValid(Player player) {
        return tile != null && tile.getLevel() != null && tile.isBatteryAssembled()
                && stillValid(ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()), player, BatteryRedstonePort.INSTANCE);
    }
    @Override public GuiSync.IGUIPacket getGuiPacket() { return tile; }
    @Override public void executeRequest(String name, Object data) { tile.runRequest(name, data); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public BlockPos machinePos() { return tile.getBlockPos(); }
}
